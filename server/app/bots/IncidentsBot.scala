package bots

import akka.util.Timeout
import info.mukel.telegrambot4s.models.{InlineKeyboardButton, InlineKeyboardMarkup}
import pme.bots.callback
import pme.bots.entity.FSMData
import shared.IncidentLevel.{IncidentLevel, MEDIUM}
import shared.IncidentStatus.{IncidentStatus, OPEN}
import shared.IncidentType.{Garage, IncidentType}
import shared.{Asset, Incident, IncidentTag}

import scala.util.Random
import scala.concurrent.duration._
import scala.language.postfixOps

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

  case class IncidentData(ident: String = Random.alphanumeric.take(4).mkString
                          , status: IncidentStatus = OPEN
                          , level: IncidentLevel = MEDIUM
                          , incidentType: IncidentType = Garage
                          , descr: String = "NOT SET"
                          , assets: List[Asset] = Nil) extends FSMData {

    def toIncident: Incident = Incident(ident, level, incidentType, descr, status, assets)

  }

  object IncidentData {
    def apply(incident: Incident): IncidentData = new IncidentData(incident.ident, incident.status, incident.level, incident.incidentType, incident.descr, incident.assets)
  }

}

