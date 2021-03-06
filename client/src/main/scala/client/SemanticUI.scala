package client

import org.scalajs.jquery.JQuery
import shared.IncidentLevel.{INFO, MEDIUM, URGENT}
import shared.IncidentType.{Elevator, Garage, Heating, Other, Water}

import scala.language.implicitConversions
import scala.scalajs.js
import shared.{IncidentLevel, IncidentStatus, IncidentTag, IncidentType}

/**
  * Created by rendong on 17/1/2.
  */
object SemanticUI {

  // Monkey patching JQuery
  @js.native
  trait SemanticJQuery extends JQuery {

    def dropdown(params: js.Any*): SemanticJQuery = js.native
    def modal(params: js.Any*): SemanticJQuery = js.native

  }

  // Monkey patching JQuery with implicit conversion
  implicit def jq2semantic(jq: JQuery): SemanticJQuery = jq.asInstanceOf[SemanticJQuery]


  def cssClass(tag: IncidentTag): String = tag match {
    case level: IncidentLevel => levelClass(level)
    case status: IncidentStatus => statusClass(status)
    case incType: IncidentType => typeClass(incType)
  }

  import IncidentLevel._

  def levelClass(level: IncidentLevel): String = level match {
    case URGENT => "red warning circle icon"
    case MEDIUM => "orange warning sign icon"
    case INFO => "blue info circle icon"
  }

  import shared.IncidentStatus._

  def statusClass(inType: IncidentStatus): String = inType match {
    case OPEN => "red warning icon"
    case IN_PROGRESS => "orange spinner icon"
    case DONE => "green checkmark box icon"
  }

  def typeClass(inType: IncidentType): String = inType match {
    case Garage => "grey car icon"
    case Heating => "grey thermometer half icon"
    case Water => "grey bathtub icon"
    case Elevator => "grey sort icon"
    case Other => "grey help circle icon"
  }
}