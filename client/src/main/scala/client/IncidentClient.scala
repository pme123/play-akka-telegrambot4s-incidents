package client

import com.thoughtworks.binding.Binding.{Constants, Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.raw._
import org.scalajs.dom.{document, window}
import play.api.libs.json._
import shared.IncidentMsg.{IncidentHistory, NewIncident}
import shared._

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.timers.setTimeout

object IncidentClient extends js.JSApp {

  implicit def makeIntellijHappy(x: scala.xml.Elem): Binding[HTMLElement] = ???


  private val incidents = Vars[Incident]()
  private val editIncident = Var[Option[Incident]](None)

  private lazy val wsURL = s"ws://${window.location.host}/ws"

  private var socket: WebSocket = _

  private def connectWS() {
    socket = new WebSocket(wsURL)
    socket.onmessage = {
      (e: MessageEvent) =>
        val message = Json.parse(e.data.toString)
        message.validate[IncidentMsg] match {
          case JsSuccess(NewIncident(incident), _) =>
            addIncident(incident)
          case JsSuccess(incidentHistory: IncidentHistory, _) =>
            addIncidents(incidentHistory.incidents)
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
    socket.onopen = { (e: Event) =>
      println("websocket open!")
      incidents.value.clear()
    }
    socket.onclose = { (e: CloseEvent) =>
      println("closed socket" + e.reason)
      setTimeout(1000) {
        connectWS() // try to reconnect automatically
      }
    }
  }

  private def addIncidents(newIncidents: Seq[Incident]) {
    incidents.value.clear()
    incidents.value ++= newIncidents

    // make sure the top of the list is visible
    val objDiv = document.getElementById("incident-panel")
    objDiv.scrollTop = objDiv.scrollHeight - newIncidents.length * 20
  }

  private def addIncident(incident: Incident) {
    incidents.value.insert(0, incident)

    val objDiv = document.getElementById("incident-panel")
    objDiv.scrollTop = objDiv.scrollHeight - 20
  }

  @dom
  private def incidentDiv(incident: Incident) =
    <div class="incident-row">
      {renderTag(incident.level, "incident-level").bind}{//
      renderTag(incident.status, "incident-status").bind}{//
      renderTag(incident.incidentType, "incident-type").bind}{//
      renderIdent(incident.ident).bind}
      <button class="incident-show-detail" onclick={event: Event => editIncident.value = Some(incident)}>
        Show Details
      </button>
      <div class="incident-descr">
        {incident.descr}
      </div>

    </div>

  @dom
  private def render = {
    <div class="main-panel">
      <div class="button-panel">
        <button onclick={event: Event => incidents.value.clear()}>
          Clear Console
        </button>
      </div>{renderIncidents.bind}{renderDetail.bind}
    </div>
  }

  @dom
  private def renderIncidents = {
    val incs = incidents.bind
    <div id="incident-panel">
      {Constants(incs: _*).map(incidentDiv(_).bind)}
    </div>
  }

  @dom
  private def renderDetail = {
    val maybeInc = editIncident.bind
    <div>
      {Constants(maybeInc.toSeq: _*).map(inc => showDetail(inc).bind)}
    </div>
  }

  @dom
  private def showDetail(incident: Incident) =
    <div class="detail-background">
      <div class="main-panel detail-panel">
        <div class="incident-row">
          {renderTag(incident.level, "incident-level").bind}{//
          renderTag(incident.status, "incident-status").bind}{//
          renderTag(incident.incidentType, "incident-type").bind}{//
          renderIdent(incident.ident).bind}
          <button class="incident-show-detail" onclick={_: Event => editIncident.value = None}>
            Close
          </button>
          <div class="incident-descr">
            {incident.descr}
          </div>

        </div>
        <div class="detail-images">
          {Constants(incident.assets: _*).map(a => renderImage(a).bind)}
        </div>
      </div>
    </div>

  @dom
  private def renderTag(tag: IncidentTag, cssClass: String) =
    <div class={s"incident-tag $cssClass ${tag.name}"}>
      {tag.name}
    </div>

  @dom
  private def renderIdent(ident: String) =
    <div class={s"incident-tag ${ident}"}>
      <b>
        {ident}
      </b>
    </div>

  @dom
  private def renderImage(asset: Asset) = {
    <div class="detail-image">
      <img src={asset.path}></img>
    </div>
  }

  def main(): Unit = {
    dom.render(document.getElementById("incident-client"), render)
    connectWS() // initial population
  }
}
