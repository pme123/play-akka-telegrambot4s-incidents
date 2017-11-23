package shared

import julienrf.json.derived
import play.api.libs.json.OFormat

trait IncidentTag {
  def name: String

  def label: String = name

 // require(name != null && name.length >= 3, "A name is required to have at least 3 characters")

}

object IncidentTag {

  //implicit lazy val jsonFormat: OFormat[IncidentTag] = derived.oformat[IncidentTag]()
}

sealed trait IncidentType extends IncidentTag {
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

  def typeFrom(name: String): IncidentType = name match {
    case Heating.name => Heating
    case Water.name => Water
    case Elevator.name => Elevator
    case Garage.name => Garage
    case Other.name => Other
    case other => throw new IllegalArgumentException(s"Unsupported IncidentType: $other")
  }

  implicit val jsonFormat: OFormat[IncidentType] = derived.oformat[IncidentType]()

}

sealed trait IncidentStatus extends IncidentTag {
}

object IncidentStatus {

  case object OPEN extends IncidentStatus {
    val name = "OPEN"
  }

  case object IN_PROGRESS extends IncidentStatus {
    val name = "IN_PROGRESS"
    override val label = "IN PROGRESS"
  }

  case object DONE extends IncidentStatus {
    val name = "DONE"
  }

  val all = Seq(OPEN, IN_PROGRESS, DONE)

  def statusFrom(name: String): IncidentStatus = name match {
    case OPEN.name => OPEN
    case IN_PROGRESS.name => IN_PROGRESS
    case DONE.name => DONE
    case other => throw new IllegalArgumentException(s"Unsupported IncidentStatus: $other")
  }

  implicit val jsonFormat: OFormat[IncidentStatus] = derived.oformat[IncidentStatus]()

}

sealed trait IncidentLevel extends IncidentTag {
  def >=(level: IncidentLevel): Boolean
}

object IncidentLevel {

  case object URGENT extends IncidentLevel {
    val name = "URGENT"

    def >=(level: IncidentLevel): Boolean = true
  }

  case object MEDIUM extends IncidentLevel {
    val name = "MEDIUM"

    def >=(level: IncidentLevel): Boolean = level != URGENT
  }

  case object INFO extends IncidentLevel {
    val name = "INFO"

    def >=(level: IncidentLevel): Boolean = level == INFO
  }

  def all = Seq(INFO, MEDIUM, URGENT)

  def levelFrom(name: String): IncidentLevel = name match {
    case URGENT.name => URGENT
    case MEDIUM.name => MEDIUM
    case INFO.name => INFO
    case other => throw new IllegalArgumentException(s"Unsupported IncidentLevel: $other")
  }

  implicit val jsonFormat: OFormat[IncidentLevel] = derived.oformat[IncidentLevel]()

}