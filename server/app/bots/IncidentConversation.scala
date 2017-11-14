package bots

import javax.inject.{Inject, Named, Singleton}

import akka.actor.{ActorRef, ActorSystem, Props}
import info.mukel.telegrambot4s.models.{InlineKeyboardButton, InlineKeyboardMarkup}
import pme.bots.callback
import pme.bots.control.ChatConversation
import pme.bots.entity.SubscrType.SubscrConversation
import pme.bots.entity.{Command, FSMData, FSMState, Subscription}
import shared.IncidentType._
import shared.IncidentLevel.MEDIUM
import shared.IncidentStatus.OPEN
import shared.{Asset, Incident}

import scala.concurrent.ExecutionContext
import scala.util.Random

// @formatter:off
/**
  * report an incident with an IncidentType, a description and optional images.
  *
  *     [Idle]  <-------------
  *       v                  |
  *   [SelectIncidentType]   |
  *       v                  |
  *    [AddDescription]      |
  *       v                  |
  *   [AddAdditionalInfo] <--|
  *       v                  |
  *       --------------------
  */
// @formatter:on
class IncidentConversation(incidentActor: ActorRef)
                          (implicit ec: ExecutionContext)
  extends ChatConversation {

  private val finishReportTag = "Finish Report"

  // if no Conversation is active - the Conversation is in the Idle state
  when(Idle) {
    case Event(Command(msg, _), _) =>
      // the message contains only the command '/incidents' - so msg is only needed for the response.
      bot.sendMessage(msg, "Please select incident type!"
        // create the buttons for all IncidentTypes
        , Some(incidentSelector))
      // tell where to go next - we don't have any state
      goto(SelectIncidentType)
    // always handle all possible requests
    case other => notExpectedData(other)
  }

  // first step after selecting IncidentType.
  when(SelectIncidentType) {
    case Event(Command(msg, callbackData: Option[String]), _) =>
      // now we check the callback data
      callbackData match {
        case Some(data) =>
          // ask the user for a description, as it is a text input no markup is needed.
          bot.sendMessage(msg, "Add a description:")
          // when we go to the next step we add the IncidentType to the FSM.
          goto(AddDescription) using IncidentTypeData(typeFrom(data))
        case None =>
          // when the user does not press a button - remind the user what we need
          bot.sendMessage(msg, "First you have to select the incident type!"
            , Some(incidentSelector))
          // and stay where we are
          stay()
      }
  }

  when(AddDescription) {
    // now we always work with the state of the previous step
    case Event(Command(msg, _), IncidentTypeData(incidentType)) =>
      // all from the text input is in msg.text
      msg.text match {
        // check if the description has at least 5 characters
        case Some(descr) if descr.length >= 5 =>
          // ask for photos and provide a button to finish the report
          bot.sendMessage(msg, "You can now add a Photo or finish the report!"
            , bot.createDefaultButtons(finishReportTag)
          )
          // now the state contains the IncidentType and the description
          goto(AddAdditionalInfo) using IncidentData(incidentType, descr)
        case _ =>
          // in any other case try to bring the user back on track
          bot.sendMessage(msg, "The description needs to have at least 5 characters!")
          stay()
      }
  }

  when(AddAdditionalInfo) {
    case Event(Command(msg, callbackData: Option[String]), incidentData: IncidentData) =>
      callbackData match {
        // first check if the user hit the 'finish' button
        case Some(data) if data == finishReportTag =>
          // give a hint that the process is finished
          bot.sendMessage(msg, "Thanks for the Report.\n" +
            "\nIf you have another incident, click here: /incident")
          // send the Incident to the IncidentActor that informs the web-clients
          incidentActor ! incidentData.toIncident
          // go to the start step
          goto(Idle)
        case _ =>
          // the process is asynchronous so a special step is needed
          bot.getFilePath(msg).map {
            case Some((fileId, path)) =>
              // if the user added a photo - she can add more photos
              bot.sendMessage(msg, "Ok, just add another Photo or finish the Report.", bot.createDefaultButtons(finishReportTag))
              // async: the result is send to itself (ChatConversation) - the uploaded photo is added to the state.
              self ! ExecutionResult(AddAdditionalInfo, incidentData.copy(assets = Asset(fileId, path) :: incidentData.assets))
            case _ =>
              // in any other case try to bring the user back on track
              bot.sendMessage(msg, "You can only add a Photo or finish the Report.", bot.createDefaultButtons(finishReportTag))
              // async: the result is send to itself (ChatConversation) - no state change.
              self ! ExecutionResult(AddAdditionalInfo, incidentData)
          }
          // async: go to the special step (ChatConversation) - which waits until it gets the ExecutionResult
          goto(WaitingForExecution)
      }
  }

  private lazy val incidentSelector: InlineKeyboardMarkup = {
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

  private def tag(name: String): String = callback + name

  case object SelectIncidentType extends FSMState

  case object AddDescription extends FSMState

  case object AddAdditionalInfo extends FSMState

  case class IncidentTypeData(incidentType: IncidentType) extends FSMData

  case class IncidentData(incidentType: IncidentType, descr: String, assets: List[Asset] = Nil) extends FSMData {

    def toIncident: Incident = Incident(Random.alphanumeric.take(6).mkString, MEDIUM,incidentType, descr, OPEN, assets)

  }


}

object IncidentConversation {
  val command = "/incident"

  def props(incidentActor: ActorRef)(implicit ec: ExecutionContext): Props = Props(new IncidentConversation(incidentActor))
}

@Singleton
class IncidentConversationSubscription @Inject()(@Named("commandDispatcher")
                                                 commandDispatcher: ActorRef
                                                 , @Named("incidentActor") incidentActor: ActorRef
                                                 , system: ActorSystem)
                                                (implicit ec: ExecutionContext) {

  import IncidentConversation._

  commandDispatcher ! Subscription(command, SubscrConversation
    , Some(_ => system.actorOf(props(incidentActor))))

}
