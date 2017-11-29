package client

import org.scalajs.dom.raw._
import org.scalajs.dom.window
import play.api.libs.json.{JsError, JsSuccess, Json}
import shared.IncidentMsg.{IncidentHistory, NewIncident}
import shared.{Incident, IncidentMsg}

import scala.scalajs.js.timers.setTimeout

case class ClientWebsocket(uiState: UIState)
  extends UIStore {

  private lazy val wsURL = s"ws://${window.location.host}/ws"

  lazy val socket = new WebSocket(wsURL)

  def connectWS() {
    socket.onmessage = {
      (e: MessageEvent) =>
        val message = Json.parse(e.data.toString)
        message.validate[IncidentMsg] match {
          case JsSuccess(NewIncident(incident), _) =>
            addIncident(incident)
          case JsSuccess(incidentHistory: IncidentHistory, _) =>
            replaceIncidents(incidentHistory.incidents)
          case JsSuccess(other, _) =>
            println(s"Other message: $other")
          case JsError(errors) =>
            errors foreach println
        }
    }
    socket.onerror = { (e: ErrorEvent) =>
      println(s"exception with websocket: ${e.message}!")
      socket.close(0, e.message)
    }
    socket.onopen = { (_: Event) =>
      println("websocket open!")
    }
    socket.onclose = { (e: CloseEvent) =>
      println("closed socket" + e.reason)
      setTimeout(1000) {
        connectWS() // try to reconnect automatically
      }
    }
  }

  private def replaceIncidents(newIncidents: Seq[Incident]) {
    clearIncidents()
    newIncidents
      .foreach(addIncident)
  }

}
