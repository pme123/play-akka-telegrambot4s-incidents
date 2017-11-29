package client

import client.SortColumn._
import com.thoughtworks.binding.Binding.{BindingSeq, Constants}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.raw.{Event, HTMLElement}
import org.scalajs.jquery.jQuery
import shared.{Incident, IncidentTag}

import scala.scalajs.js.timers._

trait IncidentTable
  extends UIStore
    with IncidentImplicits {

  @dom
  protected def incidentTable(content: BindingSeq[HTMLElement]
                              , showDetail: Boolean = true): Binding[HTMLElement] =
    <table class="ui basic table">
      <thead>
        <tr class={if (showDetail) "show-header" else "hide-header"}>
          {sortIcon(LEVEL).bind}{//
          sortIcon(STATUS).bind}{//
          sortIcon(TYPE).bind}{//
          sortIcon(IDENT, "two", "left").bind}{//
          sortIcon(DESCR, "ten", "left").bind}<th class="one wide"></th>
        </tr>
      </thead>
      <tbody>
        {content.bind}
      </tbody>
    </table>

  @dom
  private def sortIcon(sortColumn: SortColumn
                       , columnSize: String = "one"
                       , align: String = "center") = {
    val sortClass = uiState.sort.value match {
      case Sort(sc, asc) if sortColumn == sc =>
        if (asc) "caret up" else "caret down"
      case _ =>
        "sort"
    }
    <th class={s"$columnSize wide"}>
      <div class={s"$align aligned"}
           onclick={_: Event => changeSort(sortColumn)}>
        <i class={s"small $sortClass icon"}></i>
      </div>
    </th>

  }

  @dom
  protected def incidentRow(incident: Incident
                            , showDetail: Boolean = true): Binding[HTMLElement] =
    <tr>
      {icon(incident.level, "Level").bind}{//
      icon(incident.status, "Status").bind}{//
      icon(incident.incidentType, "Type").bind}<td>
      {incident.ident}
    </td>
      <td>
        {incident.descr}
      </td>
      <td>
        {Constants((if (showDetail) Some(showDetailButton(incident)) else None).toSeq: _*)
        .map(_.bind)}
      </td>
    </tr>

  @dom
  private def showDetailButton(incident: Incident) =
    <div class="circular small ui basic icon button"
         onclick={_: Event =>
           selectIncident(incident)

           setTimeout(200) {
             import SemanticUI.jq2semantic
             jQuery(".ui.modal").modal("show")
           }}
         data:data-tooltip={s"Show the details for ${incident.ident}"}
         data:data-position="left center">
      <i class="open envelope outline icon"></i>
    </div>


  @dom
  private def icon(tag: IncidentTag, tagClass: String) =
    <td data:data-tooltip={s"The $tagClass is ${tag.label}"}
        data:data-position="right center">
      <i class={"ui large " + SemanticUI.cssClass(tag)}></i>
    </td>

}
