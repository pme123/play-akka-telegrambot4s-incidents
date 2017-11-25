package bots

import akka.util.Timeout
import info.mukel.telegrambot4s.models.{InlineKeyboardButton, InlineKeyboardMarkup, Message}
import pme.bots.callback
import pme.bots.entity.FSMData
import shared.IncidentLevel.MEDIUM
import shared.IncidentStatus.OPEN
import shared.IncidentType.Garage
import shared._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

trait IncidentsBot {

  implicit protected val timeout: Timeout = Timeout(2 seconds)


  protected def incidentTagSelector(tags: Seq[IncidentTag]): InlineKeyboardMarkup = {

    InlineKeyboardMarkup(
      tags.grouped(2).map { row =>
        row.map(t => InlineKeyboardButton.callbackData(t.label, tag(t.name)))
      }.toSeq
    )
  }

  protected def tag(name: String): String = callback + name

  // extracts the user and returns a string, like:
  // 'firstName lastName - username'
  protected def extractUser(msg: Message): String = {
    val username = msg.chat.username.map(un => s"$un").getOrElse("")
    val lastName = msg.chat.lastName.getOrElse("")
    val firstName = msg.chat.firstName.getOrElse("")
    val name = s"$firstName $lastName - $username"

    if (name.trim.length <= 1)
      "unknown user"
    else
      name.trim
  }

  case class IncidentData(ident: String = Random.alphanumeric.take(4).mkString
                          , status: IncidentStatus = OPEN
                          , level: IncidentLevel = MEDIUM
                          , incidentType: IncidentType = Garage
                          , descr: String = "NOT SET"
                          , assets: List[Asset] = Nil
                          , audits: List[Audit]) extends FSMData {

    def toIncident: Incident = Incident(ident, level, incidentType, descr, status, assets, audits)

  }

  object IncidentData {

    def apply(user: String, incidentType: IncidentType): IncidentData =
      IncidentData(incidentType = incidentType, audits = List(Audit(user))
      )

    def apply(user: String, incident: Incident): IncidentData =
      new IncidentData(
        incident.ident
        , incident.status
        , incident.level
        , incident.incidentType
        , incident.descr
        , incident.assets
        , Audit(user) :: incident.audits)
  }

}

