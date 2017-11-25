package client

import client.IncidentClient.{changeFilterLevel, changeFilterStatus, changeFilterText, changeFilterType, clearIncidents, uiState}
import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.raw.{Event, HTMLElement}
import shared.{IncidentLevel, IncidentStatus, IncidentTag, IncidentType}
import shared.IncidentTag.ALL
import scala.scalajs.js.Dynamic.{global => g}

import scala.xml.Elem

case class IncidentsHeader(uiState: UIState)
  extends UIStore
    with IncidentImplicits {

  @dom
  private[client] def adapterHeader: Binding[HTMLElement] = {
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
                changeFilterLevel(if (s"${levelFilterSelect.value}" == ALL.name) ALL
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

}
