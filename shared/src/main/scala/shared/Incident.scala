package shared

import java.time.Instant

import julienrf.json.derived
import play.api.libs.json._
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

import AuditAction._
case class Audit(user: String
                 , action: AuditAction = incidentCreated
                 , dateTime: Instant = Instant.now()
                )

object Audit {

  implicit val localInstantReads: Reads[Instant] =
    (json: JsValue) => {
      json.validate[Long]
        .map { epochSecond =>
          Instant.ofEpochSecond(epochSecond)
        }
    }

  implicit val localInstantWrites: Writes[Instant] =
    (instant: Instant) => JsNumber(instant.getEpochSecond)

  implicit val jsonFormat: OFormat[Audit] = derived.oformat[Audit]()

}


object AuditAction {
  type AuditAction = String

  val incidentCreated = "Incident Created"
  val statusChanged = "Status Changed"
  val levelChanged = "Level Changed"
  val photoAdded = "Photo Added"

}