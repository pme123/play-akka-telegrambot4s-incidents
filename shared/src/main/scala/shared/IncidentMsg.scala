package shared

import julienrf.json.derived
import play.api.libs.json.OFormat

/**
  * all needed messages for server-client communication.
  */
sealed trait IncidentMsg

object IncidentMsg {

  case class NewIncident(incident: Incident) extends IncidentMsg

  case class IncidentHistory(incidents: Seq[Incident]) extends IncidentMsg

  case object KeepAliveMsg extends IncidentMsg

  // marshalling and unmarshalling
  // with json.validate[IncidentMsg] or Json.parse(incidentMsg)
  // this line is enough with this library - as IncidentMsg is a sealed trait
  implicit val jsonFormat: OFormat[IncidentMsg] = derived.oformat[IncidentMsg]()
}