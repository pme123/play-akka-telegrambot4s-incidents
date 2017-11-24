import bots.{EditIncidentConversationSubscription, IncidentConversationSubscription}
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import pme.bots.boundary.BotRunner
import pme.bots.control.{CommandDispatcher, LogStateSubscription}
import pme.bots.examples.conversations.CounterServiceSubscription
import pme.bots.examples.services.HelloServiceSubscription

class Module extends AbstractModule with AkkaGuiceSupport {
  import actors._

  override def configure(): Unit = {
    // For the web-sockets:
    // Actor between the Bot logic and the Websockets
    bindActor[IncidentActor]("incidentActor")
    // Actor that handles the web-sockets (all web-clients)
    bindActor[UserParentActor]("userParentActor")
    // Actor of one web-socket
    bindActorFactory[UserActor, UserActor.Factory]

    // Generic for the play-akka-telegrambot4s library
    // the generic CommandDispatcher
    bindActor[CommandDispatcher]("commandDispatcher")
    // starts the Bot itself (Boundary)
    bind(classOf[BotRunner]).asEagerSingleton()

    // your Services:
    bind(classOf[HelloServiceSubscription]).asEagerSingleton()
    // your Conversations:
    bind(classOf[CounterServiceSubscription]).asEagerSingleton()
    bind(classOf[IncidentConversationSubscription]).asEagerSingleton()
    bind(classOf[EditIncidentConversationSubscription]).asEagerSingleton()
    // your RunAspects
    bind(classOf[LogStateSubscription]).asEagerSingleton()
  }
}
