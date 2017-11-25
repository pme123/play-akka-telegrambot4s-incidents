package client

import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.raw.HTMLElement
import shared.{Asset, Audit, Incident}

private[client] case class IncidentDetailView(incident: Incident, uiState: UIState)
  extends IncidentImplicits
    with IncidentTable {

  @dom
  private[client] def showDetail(): Binding[HTMLElement] =
    <div class="detail-view ui modal">
      <i class="close icon"></i>
      <div class="header">
        {incidentTable(
        Constants(incident)
          .map(incidentRow(_, showDetail = false).bind)
        , showDetail = false).bind}
      </div> <div class="header">
      History
    </div> <div class="content">
      {Constants(incident.audits: _*).map(renderAudit(_).bind) //
      }
    </div><div class="ui header">
      Photos
    </div>{//
      if (incident.assets.isEmpty)
        Constants(incident).map(_ => noPhotos().bind)
      else
        Constants(incident.assets: _*).map(renderImage(_).bind) //
      }
    </div>

  @dom
  private def noPhotos() =
    <div class="content">
      No Photos attached
    </div>

  @dom
  private def renderImage(asset: Asset) = {
    <div class="image content">
      <img src={asset.path}></img>
    </div>
  }

  @dom
  private def renderAudit(audit: Audit) =
    <div class="ui basic grid">
      <div class="five wide column">
        {s"User: ${audit.user}"}
      </div>
      <div class="five wide column">
        {s"Modification Date: TODO"}
      </div>
      <div class="six wide column">
        {s"Action: TODO"}
      </div>
    </div>
}
