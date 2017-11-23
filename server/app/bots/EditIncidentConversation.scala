package bots

import javax.inject.{Inject, Named, Singleton}

import actors.IncidentActor.IncidentIdent
import akka.actor.FSM.Failure
import akka.actor.Status.Success
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import info.mukel.telegrambot4s.models.Message
import pme.bots.control.ChatConversation
import pme.bots.entity.SubscrType.SubscrConversation
import pme.bots.entity.{Command, FSMState, Subscription}
import shared._

import scala.concurrent.ExecutionContext
import scala.util.Try

// @formatter:off
/**
  * edit an incident - chosen by its ident.
  *
  *     [Idle]  <---------------
  *       v                    |
  *   [SelectIncident]         |
  *            v               |
  *          [SelectAction]<-| |
  *          v       |   |   | |
  * [ChangeStatus]   v   |   | |
  *   |  [ChangeStatus]  v   | |
  *   |   |   [ChangeStatus] | |
  *   v   v         v        | |
  *   --------------------------
  */
// @formatter:on
class EditIncidentConversation(incidentActor: ActorRef)
                              (implicit ec: ExecutionContext)
  extends ChatConversation
    with IncidentsBot {

  // if no Conversation is active - the Conversation is in the Idle state
  when(Idle) {
    case Event(Command(msg, _), _) =>
      // the message contains only the command '/editincident' - so msg is only needed for the response.
      bot.sendMessage(msg, "Please send me the ident of the Incident.")
      // tell where to go next - we don't have any state
      goto(SelectIncident)
    // always handle all possible requests
    case other => notExpectedData(other)
  }

  when(SelectIncident) {
    case Event(Command(msg, _), _) =>
      info(s"msg for ident: ${msg.text} - $self")
      msg.text match {
        case Some(ident) =>
          (incidentActor ? IncidentIdent(ident)).map {
            case Some(incident: Incident) =>
              bot.sendMessage(msg, s"Ok what do you want to do?", editIncidentMarkup)
              self ! ExecutionResult(SelectAction, IncidentData(incident))
            case Some(other) =>
              other match {
                case Success(unexpected) =>
                  warn(s"Unexpected response from IncidentActor: $unexpected")
                case Failure(exc: Exception) =>
                  error(exc, s"Problem getting Incident for ident: $ident")
              }
              bot.sendMessage(msg, s"There is no Incident with the ident $ident! Please try again.")
              self ! ExecutionResult(SelectIncident, NoData)
          }.recover {
            case exc: Exception =>
              error(exc, "Exception asking for the incident.")
              self ! ExecutionResult(SelectIncident, NoData)
          }
          goto(WaitingForExecution)
        case _ =>
          // in any other case try to bring the user back on track
          bot.sendMessage(msg, "Please send first the ident of the Incident!")
          stay()
      }
  }

  import IncidentEditAction._

  when(SelectAction) {

    case Event(Command(msg, callbackData: Option[String]), incidentData: IncidentData) =>
      // now we check the callback data
      callbackData match {
        case Some(CHANGE_STATUS.name) =>
          bot.sendMessage(msg, s"Change the Incident Status from ${incidentData.status}:"
            , statusMarkup(incidentData))
          goto(ChangeStatus) using incidentData
        case Some(CHANGE_LEVEL.name) =>
          bot.sendMessage(msg, s"Change the Incident Level from ${incidentData.level}:"
            , levelMarkup(incidentData))
          goto(ChangeLevel) using incidentData
        case Some(CHANGE_PHOTO.name) =>
          // ask the user for a description, as it is a text input no markup is needed.
          bot.sendMessage(msg, s"Add another photo")
          goto(AddPhoto) using incidentData
        case other =>
          warn(s"unexpected user input: $other")
          bot.sendMessage(msg, "First you have to select the action you want to do!"
            , editIncidentMarkup)
          // and stay where we are
          stay() using incidentData
      }
  }

  when(ChangeStatus) {
    case Event(Command(msg, callbackData: Option[String]), incidentData: IncidentData) =>
      callbackData match {
        case Some(statusName) =>
          Try(IncidentStatus.statusFrom(statusName))
            .map { st =>
              bot.sendMessage(msg, s"We changed the status to $st.")
              val newData = incidentData.copy(status = st)
              lastStep(msg, newData)
              goto(SelectAction) using newData
            }.recover {
            case exc: Exception =>
              error(exc, s"There was an exception when changing the IncidentStatus for $incidentData")
              stay() using incidentData
          }.get

        case _ =>
          // in any other case try to bring the user back on track
          bot.sendMessage(msg, "Please choose a status!", statusMarkup(incidentData))
          stay() using incidentData
      }
  }

  when(ChangeLevel) {
    case Event(Command(msg, callbackData: Option[String]), incidentData: IncidentData) =>
      callbackData match {
        case Some(levelName) =>
          Try(IncidentLevel.levelFrom(levelName))
            .map { le =>
              bot.sendMessage(msg, s"We changed the level to $le.")
              val newData = incidentData.copy(level = le)
              lastStep(msg, newData)
              goto(SelectAction) using newData
            }.recover {
            case exc: Exception =>
              error(exc, s"There was an exception when changing the IncidentStatus for $incidentData")
              stay() using incidentData
          }.get

        case _ =>
          // in any other case try to bring the user back on track
          bot.sendMessage(msg, "Please choose the level!", levelMarkup(incidentData))
          stay() using incidentData
      }
  }

  when(AddPhoto) {
    case Event(Command(msg, _), incidentData: IncidentData) =>
      // the process is asynchronous so a special step is needed
      bot.getFilePath(msg).map {
        case Some((fileId, path)) =>
          // if the user added a photo - she can add more photos
          bot.sendMessage(msg, "Ok, just add another Photo or finish the Report.")
          val newData = incidentData.copy(assets = Asset(fileId, path) :: incidentData.assets)
          lastStep(msg, newData)
          // async: the result is send to itself (ChatConversation) - the uploaded photo is added to the state.
          self ! ExecutionResult(SelectAction, newData)
        case _ =>
          // in any other case try to bring the user back on track
          bot.sendMessage(msg, "You can only add a Photo.")
          // async: the result is send to itself (ChatConversation) - no state change.
          self ! ExecutionResult(AddPhoto, incidentData)
      }
      // async: go to the special step (ChatConversation) - which waits until it gets the ExecutionResult
      goto(WaitingForExecution)
  }

  private lazy val editIncidentMarkup = Some(incidentTagSelector(IncidentEditAction.all))

  trait IncidentEditAction extends IncidentTag

  object IncidentEditAction {
    type IncidentEditAction = IncidentTag

    case object CHANGE_STATUS extends IncidentEditAction {
      val name = "changeStatus"
      override val label = "Change Status"
    }

    case object CHANGE_LEVEL extends IncidentEditAction {
      val name = "changeLevel"
      override val label = "Change Level"
    }

    case object CHANGE_PHOTO extends IncidentEditAction {
      val name = "addPhoto"
      override val label = "add Photo"
    }

    val all = Seq(CHANGE_STATUS, CHANGE_LEVEL, CHANGE_PHOTO)

    def actionFrom(name: String): IncidentEditAction = name match {
      case CHANGE_STATUS.name => CHANGE_STATUS
      case CHANGE_LEVEL.name => CHANGE_LEVEL
      case CHANGE_PHOTO.name => CHANGE_PHOTO
      case other => throw new IllegalArgumentException(s"Unsupported IncidentLevel: $other")
    }
  }

  private def statusMarkup(incidentData: IncidentData) =
    Some(incidentTagSelector(IncidentStatus.all.filterNot(_ == incidentData.status)))

  private def levelMarkup(incidentData: IncidentData) =
    Some(incidentTagSelector(IncidentLevel.all.filterNot(_ == incidentData.level)))

  private def lastStep(msg: Message, newData: IncidentData) {
    bot.sendMessage(msg
      , s"\nPress ${EditIncidentConversation.command} to edit another incident." +
        s"\nIf you have more changes to the Incident ${newData.ident} press the button."
      , editIncidentMarkup)
    incidentActor ! newData.toIncident
  }

  case object SelectIncident extends FSMState

  case object SelectAction extends FSMState

  case object ChangeStatus extends FSMState

  case object ChangeLevel extends FSMState

  case object AddPhoto extends FSMState


}

object EditIncidentConversation {
  val command = "/editincident"

  def props(incidentActor: ActorRef)(implicit ec: ExecutionContext): Props = Props(new EditIncidentConversation(incidentActor))
}

@Singleton
class EditIncidentConversationSubscription @Inject()(@Named("commandDispatcher") commandDispatcher: ActorRef
                                                     , @Named("incidentActor") incidentActor: ActorRef
                                                     , system: ActorSystem)
                                                    (implicit ec: ExecutionContext) {

  import EditIncidentConversation._

  commandDispatcher ! Subscription(command, SubscrConversation
    , Some(_ => system.actorOf(props(incidentActor))))

}
