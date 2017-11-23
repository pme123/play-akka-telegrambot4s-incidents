package client

import com.thoughtworks.binding.Binding.{Var, Vars}
import shared.{Incident, IncidentLevel}

trait UIStore {
  protected def uiState: UIState

  protected def clearIncidents() {
    println("UIStore: clearIncidents")
    uiState.incidents.value.clear()
  }

  protected def addIncident(incident: Incident) {
    println(s"UIStore: addIncident $incident")
    uiState.incidents.value += incident
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

  protected def changeFilterLevel(level: IncidentLevel) {
    println(s"UIStore: changeFilterLevel $level")
    uiState.filterLevel.value = level
  }


}

case class UIState(incidents: Vars[Incident] = Vars[Incident]()
                   , editIncident: Var[Option[Incident]] = Var[Option[Incident]](None)
                   , filterText: Var[String] = Var[String]("")
                   , filterLevel: Var[IncidentLevel] = Var[IncidentLevel](IncidentLevel.INFO)
                  )
