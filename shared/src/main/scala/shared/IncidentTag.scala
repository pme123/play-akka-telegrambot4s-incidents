package shared

import julienrf.json.derived
import play.api.libs.json.OFormat

case class IncidentTag(name: String, labelOpt: Option[String] = None) {
  require(name != null && name.length >= 3, "A name is required to have at least 3 characters")
  val label: String = labelOpt.getOrElse(name)
}

object IncidentTag {

  implicit val jsonFormat: OFormat[IncidentTag] = derived.oformat[IncidentTag]()

}

object IncidentType {

  type IncidentType = IncidentTag

  val Heating: IncidentType = IncidentTag("Heating")
  val Water: IncidentType = IncidentTag("Water")
  val Elevator: IncidentType = IncidentTag("Elevator")
  val Garage: IncidentType = IncidentTag("Garage")
  val Other: IncidentType = IncidentTag("Other")

  def typeFrom(name: String): IncidentType = name match {
    case Heating.name => Heating
    case Water.name => Water
    case Elevator.name => Elevator
    case Garage.name => Garage
    case Other.name => Other
    case other => throw new IllegalArgumentException(s"Unsupported IncidentType: $other")
  }

}


object IncidentStatus {
  type IncidentStatus = IncidentTag

  val OPEN: IncidentStatus = IncidentTag("OPEN")
  val IN_PROGRESS: IncidentStatus = IncidentTag("IN_PROGRESS", Some("IN PROGRESS"))
  val DONE: IncidentStatus = IncidentTag("DONE")

  def statusFrom(name: String): IncidentStatus = name match {
    case OPEN.name => OPEN
    case IN_PROGRESS.name => IN_PROGRESS
    case DONE.name => DONE
    case other => throw new IllegalArgumentException(s"Unsupported IncidentStatus: $other")
  }
}

object IncidentLevel {
  type IncidentLevel = IncidentTag

  val URGENT: IncidentLevel = IncidentTag("URGENT")
  val MEDIUM: IncidentLevel = IncidentTag("MEDIUM")
  val INFO: IncidentLevel = IncidentTag("INFO")

  def levelFrom(name: String): IncidentLevel = name match {
    case URGENT.name => URGENT
    case MEDIUM.name => MEDIUM
    case INFO.name => INFO
    case other => throw new IllegalArgumentException(s"Unsupported IncidentLevel: $other")
  }
}