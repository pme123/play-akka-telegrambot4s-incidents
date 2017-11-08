import bots.IncidentConversationSubscription
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import pme.bots.boundary.BotRunner
import pme.bots.control.{CommandDispatcher, LogStateSubscription}
import pme.bots.examples.conversations.CounterServiceSubscription
import pme.bots.examples.services.HelloServiceSubscription

class Module extends AbstractModule with AkkaGuiceSupport {
  import actors._

  override def configure(): Unit = {
    bindActor[IncidentActor]("incidentActor")
    bindActor[UserParentActor]("userParentActor")
    bindActorFactory[UserActor, UserActor.Factory]

    bindActor[CommandDispatcher]("commandDispatcher")
    bind(classOf[BotRunner]).asEagerSingleton()
    bind(classOf[IncidentConversationSubscription]).asEagerSingleton()

    bind(classOf[HelloServiceSubscription]).asEagerSingleton()
    bind(classOf[CounterServiceSubscription]).asEagerSingleton()
    bind(classOf[LogStateSubscription]).asEagerSingleton()
  }
}
