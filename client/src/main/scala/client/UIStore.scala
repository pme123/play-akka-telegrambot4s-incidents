package client

import client.SortColumn.LEVEL
import com.thoughtworks.binding.Binding.{Var, Vars}
import shared._

trait UIStore {
  protected def uiState: UIState

  protected def clearIncidents() {
    println("UIStore: clearIncidents")
    uiState.incidents.value.clear()
  }

  protected def addIncident(incident: Incident) {
    println(s"UIStore: addIncident $incident")
    uiState.incidents.value.filter(_.ident == incident.ident)
      .foreach(uiState.incidents.value -= _)
    uiState.incidents.value.insert(0, incident)
  }

  protected def selectIncident(incident: Incident) {
    println(s"UIStore: selectIncident $incident")
    uiState.editIncident.value = Some(incident)
  }

  protected def clearEditIncident(): Unit = {
    println(s"UIStore: clearEditIncident")
    uiState.editIncident.value = None
  }

  protected def changeFilterText(text: String) {
    println(s"UIStore: changeFilterText $text")
    uiState.filterText.value = text
  }

  protected def changeFilterLevel(level: IncidentTag) {
    println(s"UIStore: changeFilterLevel $level")
    uiState.filterLevel.value = level
  }

  protected def changeFilterType(incType: IncidentTag) {
    println(s"UIStore: changeFilterType $incType")
    uiState.filterType.value = incType
  }

  protected def changeFilterStatus(status: IncidentTag) {
    println(s"UIStore: changeFilterStatus $status")
    uiState.filterStatus.value = status
  }

  protected def changeSort(sortColumn: SortColumn) {
    println(s"UIStore: changeSort $sortColumn")
    val isAsc = uiState.sort.value match {
      case Sort(sc, asc) if sortColumn == sc =>
        !asc
      case _ =>
        true
    }
    uiState.sort.value = Sort(sortColumn, isAsc)
  }

}

case class UIState(incidents: Vars[Incident] = Vars[Incident]()
                   , editIncident: Var[Option[Incident]] = Var[Option[Incident]](None)
                   , filterText: Var[String] = Var[String]("")
                   , filterLevel: Var[IncidentTag] = Var[IncidentTag](IncidentLevel.INFO)
                   , filterType: Var[IncidentTag] = Var[IncidentTag](IncidentTag.ALL)
                   , filterStatus: Var[IncidentTag] = Var[IncidentTag](IncidentTag.ALL)
                   , sort: Var[Sort] = Var[Sort](Sort())
                  )

sealed trait SortColumn {
  def sort(a: Incident, b: Incident): Boolean

}

object SortColumn {

  case object LEVEL extends SortColumn {
    def sort(a: Incident, b: Incident): Boolean = a.level.isBefore(b.level)
  }

  case object STATUS extends SortColumn {
    def sort(a: Incident, b: Incident): Boolean = a.status.isBefore(b.status)
  }

  case object TYPE extends SortColumn {
    def sort(a: Incident, b: Incident): Boolean = a.incidentType.isBefore(b.incidentType)
  }

  case object IDENT extends SortColumn {
    def sort(a: Incident, b: Incident): Boolean = a.ident.compareToIgnoreCase(b.ident) <= 0
  }

  case object DESCR extends SortColumn {
    def sort(a: Incident, b: Incident): Boolean = a.descr.compareToIgnoreCase(b.descr) <= 0
  }

}

case class Sort(sortColumn: SortColumn = LEVEL
                , isAsc: Boolean = true) {

  def sort(a: Incident, b: Incident): Boolean =
    if (isAsc)
      sortColumn.sort(a, b)
    else
      sortColumn.sort(b, a)

}