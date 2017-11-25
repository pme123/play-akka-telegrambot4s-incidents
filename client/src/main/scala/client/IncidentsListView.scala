package client

import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.dom
import shared.IncidentTag.ALL

case class IncidentsListView(uiState: UIState)
  extends IncidentImplicits
    with IncidentTable {

  @dom
  private[client] def incidentContainer = {
    val incidents = uiState.incidents.bind
    val text = uiState.filterText.bind
    val level = uiState.filterLevel.bind
    val status = uiState.filterStatus.bind
    val incType = uiState.filterType.bind
    val sort = uiState.sort.bind
    println(s"sort: $sort")
    val filteredIncidents =
      incidents
        .filter(in => in.level.filter(level) || level == ALL)
        .filter(in => in.status.filter(status) || status == ALL)
        .filter(in => in.incidentType.filter(incType) || incType == ALL)
        .filter(in => in.descr.toLowerCase.contains(text.toLowerCase)
          || in.ident.toLowerCase.contains(text.toLowerCase))
        .sortWith((a, b) => sort.sort(a, b))

    <div id="incident-panel" class="ui main text container">
      {incidentTable(Constants(filteredIncidents: _*).map(incidentRow(_).bind)).bind}
    </div>
  }

}
