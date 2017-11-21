package client

import org.scalajs.jquery.JQuery

import scala.language.implicitConversions
import scala.scalajs.js

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

}