package client

import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.document
import org.scalajs.dom.raw._
import org.scalajs.jquery.jQuery
import shared.IncidentLevel.INFO
import shared.IncidentStatus.IN_PROGRESS
import shared.IncidentTag.ALL
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
        statusFilter.bind}{//
        typeFilter.bind}{//
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
    <div class="ui item"
         data:data-tooltip="Filter the Incidents by its Level"
         data:data-position="bottom right">
      <select id="levelFilterSelect"
              class="ui compact dropdown"
              onchange={_: Event =>
                changeFilterLevel( if (s"${levelFilterSelect.value}" == ALL.name) ALL
                else
                  IncidentLevel.levelFrom(s"${levelFilterSelect.value}"))}>
        {filterOptions(IncidentLevel.all, uiState.filterLevel.value).bind}
      </select>
    </div>
  }

  @dom
  private def statusFilter = {
    <div class="ui item"
         data:data-tooltip="Filter the Incidents by its Level"
         data:data-position="bottom right">
      <select id="statusFilterSelect"
              class="ui compact dropdown"
              onchange={_: Event =>
                changeFilterStatus(
                  if (s"${statusFilterSelect.value}" == ALL.name) ALL
                  else
                    IncidentStatus.statusFrom(s"${statusFilterSelect.value}"))}>
        {filterOptions(IncidentStatus.all, uiState.filterStatus.value).bind}
      </select>
    </div>
  }

  @dom
  private def typeFilter = {
    implicit def stringToBoolean(str: String): Boolean = str == "true"

    <div class="ui item"
         data:data-tooltip="Filter the Incidents by its Level"
         data:data-position="bottom right">
      <select id="typeFilterSelect"
              class="ui compact dropdown"
              onchange={_: Event =>
                changeFilterType(
                  if (s"${typeFilterSelect.value}" == ALL.name) ALL
                  else
                    IncidentType.typeFrom(s"${typeFilterSelect.value}")
                )}>
        {filterOptions(IncidentType.all, uiState.filterType.value).bind}
      </select>
    </div>
  }

  @dom
  private def filterOptions(tags: Seq[IncidentTag]
                            , selected: IncidentTag) = {
    Constants(
      (tags :+ ALL).map(l => tagOption(l, selected))
        : _*).map(_.bind)
  }

  @dom
  private def tagOption(l: IncidentTag, selected: IncidentTag) = {
    <option value={l.name} selected={l == selected}>
      {l.label}
    </option>
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
    val status = uiState.filterStatus.bind
    val incType = uiState.filterType.bind
    val filteredIncidents =
      incidents
        .filter(in => in.level.filter(level) || level == ALL)
        .filter(in => in.status.filter(status) || status == ALL)
        .filter(in => in.incidentType.filter(incType) || incType == ALL)
        .filter(in => in.descr.toLowerCase.contains(text.toLowerCase)
          || in.ident.toLowerCase.contains(text.toLowerCase))

    <div id="incident-panel" class="ui main text container">

      <table class="ui sortable basic table">
        <thead>
          <tr class="tree-header">
            <th class="one wide"></th>
            <th class="one wide"></th>
            <th class="one wide"></th>
            <th class="two wide"></th>
            <th class="ten wide"></th>
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
      <td data:data-tooltip={s"The level is ${incident.level.label}"}
          data:data-position="right center">
        <i class={"ui large " + SemanticUI.levelClass(incident.level)}></i>
      </td>
      <td data:data-tooltip={s"The status is ${incident.status.label}"}
          data:data-position="right center">
        <i class={"ui large " + SemanticUI.statusClass(incident.status)}></i>
      </td>
      <td data:data-tooltip={s"The type is ${incident.incidentType.label}"}
          data:data-position="right center">
        <i class={"ui large " + SemanticUI.typeClass(incident.incidentType)}></i>
      </td>
      <td>
        {incident.ident}
      </td>
      <td>
        {incident.descr}
      </td>
      <td>
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
