package actors

import java.time.Instant
import javax.inject.Inject

import actors.IncidentActor.{IncidentIdent, SubscribeIncident, UnSubscribeIncident}
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import akka.stream.Materializer
import shared.IncidentMsg.{IncidentHistory, NewIncident}
import shared._

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

  incidents += Incident("dEr4s", IncidentLevel.URGENT, IncidentType.Garage, "Problem with the light."
    , IncidentStatus.IN_PROGRESS, audits = List(Audit("Peter Starck (pstark)", AuditAction.statusChanged)
      , Audit("Hans (hans123)", AuditAction.levelChanged), Audit("unknown user")))
  incidents += Incident("r5hTr", IncidentLevel.MEDIUM, IncidentType.Elevator, "Strange noise when running."
    , audits = List(Audit("Peter Starck (pstark)")))
  incidents += Incident("pT444", IncidentLevel.INFO, IncidentType.Other, "Loud music after 22:00h in Apartment 23c."
    , audits = List(Audit("Peter Starck (pstark)")))
  incidents += Incident("aZbcR", IncidentLevel.URGENT, IncidentType.Heating, "We are freezing! Fam. Meier from the house 23 on level 23."
    , audits = List(Audit("Peter Starck (pstark)")))
  incidents += Incident("ZrW36", IncidentLevel.MEDIUM, IncidentType.Water, "Cold water in the bathroom of 45c.", IncidentStatus.DONE
    , audits = List(Audit("Peter Starck (pstark)")))

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
      // bot asks for Incident for an ident
    case IncidentIdent(ident) =>
      log.info(s"requested incident for: $ident:: ${sender}")

      sender ! incidents.find(_.ident == ident)
    // there is one message that is not handled (KeepAliveMsg)
    case other =>
      log.info(s"unexpected message: $other")
  }

  // add the new Incident to the history and inform all the clients
  private def newIncident(incident: Incident) {
    val existingIncidents = incidents.filterNot(_.ident == incident.ident).toList
    incidents.clear()
    incidents ++= incident :: existingIncidents

    sendToSubscriber(NewIncident(incident))
  }

  private def sendToSubscriber(incidentMsg: IncidentMsg): Unit =
    clientActors.values
      .foreach(_ ! incidentMsg)

}

object IncidentActor {

  case class SubscribeIncident(clientId: String, wsActor: ActorRef)

  case class UnSubscribeIncident(clientId: String)

  case class IncidentIdent(ident: String)

}
