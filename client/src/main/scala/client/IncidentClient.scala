package client

import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.document
import org.scalajs.dom.raw._
import org.scalajs.jquery.jQuery
import shared._

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

object IncidentClient
  extends js.JSApp
    with UIStore {

  val uiState = UIState()

  private lazy val socket = ClientWebsocket(uiState)

  @dom
  private def incidentDiv(incident: Incident) =
    <div class="incident-row">
      {renderTag(incident.level, "incident-level").bind}{//
      renderTag(incident.status, "incident-status").bind}{//
      renderTag(incident.incidentType, "incident-type").bind}{//
      renderIdent(incident.ident).bind}
      <button class="incident-show-detail" onclick={_: Event => selectIncident(incident)}>
        Show Details
      </button>
      <div class="incident-descr">
        {incident.descr}
      </div>

    </div>

  @dom
  private def render = {
    <div class="body-panel">
      <div class="header-panel">
        <img class="header-img" src={"" + g.jsRoutes.controllers.Assets.versioned("images/favicon.png").url}></img>
        Reactive Incident Log Demo
      </div>
      <div class="main-panel">
        <div class="button-panel">
          <button onclick={_: Event => clearIncidents()}>
            Clear Console
          </button>
        </div>{renderIncidents.bind}{renderDetail.bind}
      </div>
    </div>
  }

  @dom
  private def renderIncidents = {
    val incs = uiState.incidents.bind
    <div id="incident-panel">
      {Constants(incs: _*).map(incidentDiv(_).bind)}
    </div>
  }

  @dom
  private def renderDetail = {
    val maybeInc = uiState.editIncident.bind
    <div>
      {Constants(maybeInc.toSeq: _*).map(inc => showDetail(inc).bind)}
    </div>
  }

  @dom
  private def showDetail(incident: Incident) =
    <div class="detail-background">
      <div class="main-panel detail-panel">
        <div class="incident-row">
          {renderTag(incident.level, "incident-level").bind}{//
          renderTag(incident.status, "incident-status").bind}{//
          renderTag(incident.incidentType, "incident-type").bind}{//
          renderIdent(incident.ident).bind}
          <button class="incident-show-detail" onclick={_: Event => clearEditIncident()}>
            Close
          </button>
          <div class="incident-descr">
            {incident.descr}
          </div>

        </div>
        <div class="detail-images">
          {Constants(incident.assets: _*).map(a => renderImage(a).bind)}
        </div>
      </div>
    </div>

  @dom
  private def renderTag(tag: IncidentTag, cssClass: String) =
    <div class={s"incident-tag $cssClass ${tag.name}"}>
      {tag.label}
    </div>

  @dom
  private def renderIdent(ident: String) =
    <div class={s"incident-tag ${ident}"}>
      <b>
        {ident}
      </b>
    </div>

  @dom
  private def renderImage(asset: Asset) = {
    <div class="detail-image">
      <img src={asset.path}></img>
    </div>
  }

  def main(): Unit = {
    dom.render(document.body, render)
    socket.connectWS()
    import SemanticUI.jq2semantic
    jQuery(".ui.dropdown").dropdown(js.Dynamic.literal(on = "hover"))
  }

  implicit def makeIntellijHappy(x: scala.xml.Elem): Binding[HTMLElement] = ???

}
