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

  def main(): Unit = {
    dom.render(document.body, render)
    socket.connectWS()
    import SemanticUI.jq2semantic
    jQuery(".ui.dropdown").dropdown(js.Dynamic.literal(on = "hover"))
  }

  @dom
  private def render = {
    <div>
      {adapterHeader.bind}{//
      incidentContainer.bind}{//
      renderDetail.bind}
    </div>
  }

  @dom
  private def adapterHeader = {
    <div class="ui main fixed borderless menu">
      <div class="ui item">
        <img src={"" + g.jsRoutes.controllers.Assets.versioned("images/favicon.png").url}></img>
      </div>
      <div class="ui header item">Reactive Incident Log Demo</div>
      <div class="right menu">
        {//
        textFilter.bind}{//
        levelFilter.bind}{//
        clearButton.bind}
      </div>
    </div>
  }

  // filterInput references to the id of the input (macro magic)
  // this creates a compile exception in intellij
  @dom
  private def textFilter = {
    <div class="ui item">
      <div class="ui input"
           data:data-tooltip="Filter by the ident or the description."
           data:data-position="bottom right">
        <input id="filterInput"
               type="text"
               placeholder="Filter..."
               onkeyup={_: Event =>
                 changeFilterText(s"${filterInput.value}")}>
        </input>
      </div>
    </div>
  }

  // filterInput references to the id of the input (macro magic)
  // this creates a compile exception in intellij
  @dom
  private def levelFilter = {
    implicit def stringToBoolean(str: String): Boolean = str == "true"
    import shared.IncidentLevel._
    <div class="ui item"
         data:data-tooltip="Filter the Incidents by its Level"
         data:data-position="bottom right">
      <select id="filterSelect"
              class="ui compact dropdown"
              onchange={_: Event =>
                changeFilterLevel(IncidentLevel.levelFrom(s"${filterSelect.value}"))}>
        <option value={URGENT.name}>
          {URGENT.label}
        </option>
        <option value={MEDIUM.name}>
          {MEDIUM.label}
        </option>
        <option value={INFO.name} selected="true">
          {INFO.label}
        </option>
      </select>
    </div>
  }

  @dom
  private def clearButton = {
    <div class="ui item">
      <button class="ui basic icon button"
              onclick={_: Event => clearIncidents()}
              data:data-tooltip="Clear the console"
              data:data-position="bottom right">
        <i class="remove circle outline icon large"></i>
      </button>
    </div>
  }

  @dom
  private def incidentDiv(incident: Incident) =
    <div class="incident-row">
      {renderTag(incident.level, "incident-level").bind}{//
      renderTag(incident.status, "incident-status").bind}{//
      renderTag(incident.incidentType, "incident-type").bind}{//
      renderIdent(incident.ident).bind}<button class="incident-show-detail" onclick={_: Event => selectIncident(incident)}>
      Show Details
    </button>
      <div class="incident-descr">
        {incident.descr}
      </div>

    </div>

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
    <div class={s"incident-tag $ident"}>
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

  @dom
  private def incidentContainer = {
    val incidents = uiState.incidents.bind
    val text = uiState.filterText.bind
    val level = uiState.filterLevel.bind
    val filteredIncidents =
      incidents
        .filter(in => in.level >= level)
        .filter(in => in.descr.toLowerCase.contains(text.toLowerCase)
          || in.ident.toLowerCase.contains(text.toLowerCase))

    <div id="incident-panel" class="ui main text container">

      <table class="ui sortable basic table">
        <thead>
          <tr class="tree-header">
            <th class="one wide"></th>
            <th class="one wide"></th>
            <th class="two wide">Ident</th>
            <th class="eleven wide">Description</th>
            <th class="one wide"></th>
          </tr>
        </thead>
        <tbody>
          {Constants(filteredIncidents: _*).map(incidentRow(_).bind)}
        </tbody>
      </table>
    </div>
  }

  @dom
  private def incidentRow(incident: Incident) =
    <tr>
      <td class="one wide"
          data:data-tooltip={s"The level is ${incident.level.label}"}
          data:data-position="right center">
        <i class={"ui large " + SemanticUI.levelClass(incident.level)}></i>
      </td>
      <td class="one wide"
          data:data-tooltip={s"The status is ${incident.status.label}"}
          data:data-position="right center">
        <i class={"ui large " + SemanticUI.statusClass(incident.status)}></i>
      </td>
      <td class="two wide">
        {incident.ident}
      </td>
      <td class="five wide">
        {incident.descr}
      </td>
      <td class="one wide">
        <div class="circular small ui basic icon button"
             onclick={_: Event => selectIncident(incident)}
             data:data-tooltip={s"Show the details for ${incident.ident}"}
             data:data-position="left center">
          <i class="open envelope outline icon"></i>
        </div>
      </td>
    </tr>

  implicit def makeIntellijHappy(x: scala.xml.Elem): Binding[HTMLElement] = ???

}
