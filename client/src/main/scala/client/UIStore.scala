package client

import com.thoughtworks.binding.Binding.{Var, Vars}
import shared.Incident

trait UIStore {
  protected def uiState: UIState

  protected def clearIncidents() {
    println("UIStore: clearIncidents")
    uiState.incidents.value.clear()
  }

  protected def addIncident(newIncident: Incident) {
    println(s"UIStore: addIncident $newIncident")
    uiState.incidents.value.insert(0, newIncident)
  }

  protected def selectIncident(incident: Incident) {
    println(s"UIStore: selectIncident $incident")
    uiState.editIncident.value = Some(incident)
  }

  protected def clearEditIncident(): Unit = {
    println(s"UIStore: clearEditIncident")
    uiState.editIncident.value = None
  }


}

case class UIState(incidents: Vars[Incident] = Vars[Incident]()
                   , editIncident: Var[Option[Incident]] = Var[Option[Incident]](None)
                  )
