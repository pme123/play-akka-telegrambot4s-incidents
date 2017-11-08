package bots

import javax.inject.{Inject, Named, Singleton}

import akka.actor.{ActorRef, ActorSystem, Props}
import info.mukel.telegrambot4s.models.{InlineKeyboardButton, InlineKeyboardMarkup}
import pme.bots.control.ChatConversation
import pme.bots.entity.SubscrType.SubscrConversation
import pme.bots.entity.{Command, FSMData, FSMState, Subscription}
import shared.IncidentMsg.NewIncident
import shared.{Asset, Incident, IncidentType}

// @formatter:off
/**
  * process an incident with a category, a text and image.
  *
  *     [Idle]  <-------------
  *       v                  |
  *   [SelectIncidentType] <---  |
  *       v               |  |
  *     collectInfo  ------  |
  *       v                  |
  *      --------------------
  */
// @formatter:on
class IncidentConversation(incidentActor: ActorRef)
  extends ChatConversation {

  private val TAG = callback
  private val finishReportTag = "Finish Report"

  when(Idle) {
    case Event(Command(msg, _), _) =>
      sendMessage(msg, "Please select incident type!"
        , replyMarkup = Some(incidentSelector))
      // tell where to go next
      goto(SelectIncidentType)
    case other => notExpectedData(other)
  }

  when(SelectIncidentType) {
    case Event(Command(msg, callbackData: Option[String]), _) =>
      callbackData match {
        case Some(data) =>
          sendMessage(msg, "Can you add a description!")
          goto(AddDescription) using IncidentTypeData(IncidentType.from(data))
        case None =>
          sendMessage(msg, "First you have to select the incident type!"
            , Some(incidentSelector))
          stay()
      }
    // this is a simple conversation that stays always in the same state.
  }

  when(AddDescription) {
    case Event(Command(msg, _), IncidentTypeData(incidentType)) =>
      msg.text match {
        case Some(text) if text.length >= 5 =>
          info(s"Got description: $text")
          sendMessage(msg, "You can now add a Photo or finish the report!"
            , createDefaultButtons(finishReportTag)
          )
          goto(AddAdditionalInfo) using IncidentData(incidentType, text)
        case _ =>
          sendMessage(msg, "The description needs to have at least 5 characters!")
          stay()
      }
  }

  when(AddAdditionalInfo) {
    case Event(Command(msg, callbackData: Option[String]), incidentData: IncidentData) =>
      callbackData match {
        case Some(data) if data == finishReportTag =>
          info(s"finished report")
          incidentActor ! incidentData.toIncident
          goto(Idle)
        case _ =>
          getFilePath(msg).map {
            case Some((fileId, path)) =>
              sendMessage(msg, "Ok, just add another Photo or finish the Report.", createDefaultButtons(finishReportTag))
              info(s"uploaded mess $fileId :: $path")
              self ! ExecutionResult(AddAdditionalInfo, incidentData.copy(assets = Asset(fileId, path) :: incidentData.assets))
            case _ =>
              sendMessage(msg, "You can only add a Photo or finish the Report.", createDefaultButtons(finishReportTag))
              self ! ExecutionResult(AddAdditionalInfo, incidentData)
          }
          goto(WaitingForExecution)
      }
    // this is a simple conversation that stays always in the same state.
  }

  private lazy val incidentSelector: InlineKeyboardMarkup = {
    import shared.IncidentType._
    InlineKeyboardMarkup(Seq(
      Seq(
        InlineKeyboardButton.callbackData(Heating.name, tag(Heating.name))
        , InlineKeyboardButton.callbackData(Water.name, tag(Water.name)))
      , Seq(
        InlineKeyboardButton.callbackData(Garage.name, tag(Garage.name))
        , InlineKeyboardButton.callbackData(Elevator.name, tag(Elevator.name)))
      , Seq(
        InlineKeyboardButton.callbackData(Other.name, tag(Other.name)))
    ))
  }

  private def tag: String => String = prefixTag(TAG)

  case object SelectIncidentType extends FSMState

  case object AddDescription extends FSMState

  case object AddAdditionalInfo extends FSMState

  case class IncidentTypeData(incidentType: IncidentType) extends FSMData

  case class IncidentData(incidentType: IncidentType, descr: String, assets: List[Asset] = Nil) extends FSMData {

    def toIncident: Incident = Incident(incidentType, descr, assets)

  }


}

object IncidentConversation {
  val command = "/incident"

  def props(incidentActor: ActorRef): Props = Props(new IncidentConversation(incidentActor))
}

@Singleton
class IncidentConversationSubscription @Inject()(@Named("commandDispatcher")
                                                 commandDispatcher: ActorRef
                                                 , @Named("incidentActor") incidentActor: ActorRef
                                                 , system: ActorSystem) {

  import IncidentConversation._

  commandDispatcher ! Subscription(command, SubscrConversation
    , Some(_ => system.actorOf(props(incidentActor))))

}
