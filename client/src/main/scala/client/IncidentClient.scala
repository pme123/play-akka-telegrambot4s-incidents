package client

import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.dom
import org.scalajs.dom.document
import org.scalajs.jquery.jQuery

import scala.language.implicitConversions
import scala.scalajs.js

object IncidentClient
  extends js.JSApp
    with UIStore
    with IncidentImplicits {

  val uiState = UIState()

  private lazy val socket = ClientWebsocket(uiState)

  def main(): Unit = {
    dom.render(document.body, render)
    socket.connectWS()
    import SemanticUI.jq2semantic
    jQuery(".ui.dropdown").dropdown(js.Dynamic.literal(on = "hover"))

  }

  @dom
  private def render = {
    <div>
      {IncidentsHeader(uiState).adapterHeader.bind}{//
      IncidentsListView(uiState).incidentContainer.bind}{//
      renderDetail.bind}
    </div>
  }

  @dom
  private def renderDetail = {
    val maybeInc = uiState.editIncident.bind
    <div>
      {Constants(maybeInc.toSeq: _*).map(inc => IncidentDetailView(inc, uiState).showDetail().bind)}
    </div>
  }


}
