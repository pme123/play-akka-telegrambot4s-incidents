package client

import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.raw.HTMLElement
import shared.{Asset, Audit, Incident}

import scala.scalajs.js

private[client] case class IncidentDetailView(incident: Incident, uiState: UIState)
  extends IncidentImplicits
    with IncidentTable {

  // 1. level of abstraction
  // **************************

  @dom
  private[client] def showDetail(): Binding[HTMLElement] =
    <div class="ui modal">
      {detailHeader.bind}{//
      detailAudits.bind}{//
      detailPhotos.bind}
    </div>

  // 2. level of abstraction
  // **************************

  @dom
  private def detailHeader = <div class="header">
    {incidentTable(
      Constants(incident)
        .map(incidentRow(_, showDetail = false).bind)
      , showDetail = false).bind}
  </div>

  @dom
  private def detailAudits =
    <div class="content">
      <div class="header">
        History
      </div>
      <table class="ui basic table">
        <thead>
          <tr>
            <th class="five wide">
              <b>User:</b>
            </th>
            <th class="four wide">
              <b>Action:</b>
            </th>
            <th class="seven wide">
              <b>Modification Date:</b>
            </th>
          </tr>
        </thead>
        <tbody>
          {Constants(incident.audits: _*).map(renderAudit(_).bind)}
        </tbody>
      </table>
    </div>

  @dom
  private def detailPhotos =
    <div class="content">
      <div class="ui header">
        Photos
      </div>{//
      if (incident.assets.isEmpty)
        Constants(incident).map(_ => noPhotos().bind)
      else
        Constants(incident.assets: _*).map(renderImage(_).bind) //
      }
    </div>

  // 3. level of abstraction
  // **************************

  @dom
  private def renderAudit(audit: Audit) = {
    val date = new js.Date(1000.0 * audit.dateTime.getEpochSecond)

    <tr>
      <td>
        {audit.user}
      </td>
      <td>
        {audit.action}
      </td>
      <td>
        {s" ${date.toLocaleDateString()} ${date.toLocaleTimeString()}"}
      </td>
    </tr>
  }

  @dom
  private def noPhotos() =
    <div class="content">
      No Photos attached
    </div>

  @dom
  private def renderImage(asset: Asset) = {
    <div class="image content">
      <img src={asset.path}/>
    </div>
  }

}
