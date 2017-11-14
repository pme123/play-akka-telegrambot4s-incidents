package shared

import julienrf.json.derived
import play.api.libs.json.OFormat

import IncidentLevel.IncidentLevel
import IncidentStatus.{IncidentStatus, OPEN}
import IncidentType.IncidentType


case class Incident(ident: String
                    , level: IncidentLevel
                    , incidentType: IncidentType
                    , descr: String
                    , status: IncidentStatus = OPEN
                    , assets: List[Asset] = Nil) {
  require(descr.length > 4)
}

object Incident {
  implicit val jsonFormat: OFormat[Incident] = derived.oformat[Incident]()

}

case class Asset(fileId: String, path: String)

object Asset {
  implicit val jsonFormat: OFormat[Asset] = derived.oformat[Asset]()

}
