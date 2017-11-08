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

case class Incident(incidentType: IncidentType, descr: String, assets: List[Asset]) {
  require(descr.length > 4)
}

object Incident {
  implicit val jsonFormat: OFormat[Incident] = derived.oformat[Incident]()

}

sealed trait IncidentType {
  def name: String
}

object IncidentType {

  case object Heating extends IncidentType {
    val name = "Heating"
  }

  case object Water extends IncidentType {
    val name = "Water"
  }

  case object Elevator extends IncidentType {
    val name = "Elevator"
  }

  case object Garage extends IncidentType {
    val name = "Garage"
  }

  case object Other extends IncidentType {
    val name = "Other"
  }

  def from(name: String): IncidentType = name match {
    case Heating.name => Heating
    case Water.name => Water
    case Elevator.name => Elevator
    case Garage.name => Garage
    case Other.name => Other
    case other => throw new IllegalArgumentException(s"Unsupported IncidentType: $other")
  }

  implicit val jsonFormat: OFormat[IncidentType] = derived.oformat[IncidentType]()

}

case class Asset(fileId: String, path: String)

object Asset {
  implicit val jsonFormat: OFormat[Asset] = derived.oformat[Asset]()

}
