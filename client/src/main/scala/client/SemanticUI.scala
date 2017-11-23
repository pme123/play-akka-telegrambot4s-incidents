package client

import org.scalajs.jquery.JQuery

import scala.language.implicitConversions
import scala.scalajs.js
import shared.{IncidentLevel, IncidentStatus, IncidentType}

/**
  * Created by rendong on 17/1/2.
  */
object SemanticUI {

  // Monkey patching JQuery
  @js.native
  trait SemanticJQuery extends JQuery {

    def dropdown(params: js.Any*): SemanticJQuery = js.native
  }

  // Monkey patching JQuery with implicit conversion
  implicit def jq2semantic(jq: JQuery): SemanticJQuery = jq.asInstanceOf[SemanticJQuery]

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
}