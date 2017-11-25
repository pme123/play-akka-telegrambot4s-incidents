package shared

import julienrf.json.derived
import play.api.libs.json.OFormat
import shared.IncidentStatus.OPEN


case class Incident(ident: String
                    , level: IncidentLevel
                    , incidentType: IncidentType
                    , descr: String
                    , status: IncidentStatus = OPEN
                    , assets: List[Asset] = Nil
                    , audits: List[Audit] = Nil) {
  require(descr.length > 4)
}

object Incident {
  implicit val jsonFormat: OFormat[Incident] = derived.oformat[Incident]()

}

case class Asset(fileId: String, path: String)

object Asset {
  implicit val jsonFormat: OFormat[Asset] = derived.oformat[Asset]()

}

case class Audit(user: String
                 /*, dateTime: LocalDateTime = LocalDateTime.now()*/)

object Audit {
  implicit val jsonFormat: OFormat[Audit] = derived.oformat[Audit]()
}