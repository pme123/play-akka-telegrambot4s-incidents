package actors

import javax.inject.Inject

import actors.IncidentActor.{SubscribeIncident, UnSubscribeIncident}
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import akka.stream.Materializer
import shared.IncidentMsg.{IncidentHistory, NewIncident}
import shared.{Incident, IncidentMsg}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

/**
  * This actor stands between the conversation Processes and the UserActors.
  * During this process it will inform all clients with LogEntries.
  */
class IncidentActor @Inject()(implicit mat: Materializer, ec: ExecutionContext)
  extends Actor
    with ActorLogging {

  private val incidents: mutable.ListBuffer[Incident] = mutable.ListBuffer()

  // a map with all clients (Websocket-Actor) that needs the status about the process
  private val clientActors: mutable.Map[String, ActorRef] = mutable.Map()

  def receive = LoggingReceive {
    // subscribe a client with its id and its websocket-Actor
    // this is called when the websocket for a client is created
    case SubscribeIncident(clientId, wsActor) =>
      log.info(s"Subscribed Client: $clientId")
      val aRef = clientActors.getOrElseUpdate(clientId, wsActor)
      // return the last 20 incidents to the new client
      aRef ! IncidentHistory(incidents.take(20))
    // Unsubscribe a client(remove from the map)
    // this is called when the connection from a client websocket is closed
    case UnSubscribeIncident(clientId) =>
      log.info(s"Unsubscribe Client: $clientId")
      clientActors -= clientId
    // bot informs new incidents
    case incident: Incident =>
      log.info(s"new incident: $incident")
      newIncident(incident)
    // there is one message that is not handled (KeepAliveMsg)
    case other =>
      log.info(s"unexpected message: $other")
  }

  // add the new Incident to the history and inform all the clients
  private def newIncident(incident: Incident) {
    incidents.insert(0, incident)
    sendToSubscriber(NewIncident(incident))
  }

  private def sendToSubscriber(incidentMsg: IncidentMsg): Unit =
    clientActors.values
      .foreach(_ ! incidentMsg)

}

object IncidentActor {

  case class SubscribeIncident(clientId: String, wsActor: ActorRef)

  case class UnSubscribeIncident(clientId: String)

}
