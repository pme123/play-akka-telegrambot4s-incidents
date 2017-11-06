package actors

import javax.inject.Inject

import actors.AdapterActor.{SubscribeAdapter, UnSubscribeAdapter}
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import akka.stream.Materializer
import log.LogService
import shared.LogLevel._
import shared._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

/**
  * This actor runs the Adapter Process.
  * During this process it will inform all clients with LogEntries.
  */
class AdapterActor @Inject()(implicit mat: Materializer, ec: ExecutionContext)
  extends Actor
    with ActorLogging {

  private var logService: LogService = _

  // a flag that indicates if the process is running
  private var isRunning = false

  // a map with all clients (Websocket-Actor) that needs the status about the process
  private val clientActors: mutable.Map[String, ActorRef] = mutable.Map()

  def receive = LoggingReceive {
    // subscribe a client with its id and its websocket-Actor
    // this is called when the websocket for a client is created
    case SubscribeAdapter(clientId, wsActor) =>
      log.info(s"Subscribed Client: $clientId")
      val aRef = clientActors.getOrElseUpdate(clientId, wsActor)
      val status =
        if (isRunning)
          AdapterRunning(logService.logReport)
        else
          AdapterNotRunning(if (logService != null) Some(logService.logReport) else None)
      // inform the client about the actual status
      aRef ! status
    // Unsubscribe a client(remove from the map)
    // this is called when the connection from a client websocket is closed
    case UnSubscribeAdapter(clientId) =>
      log.info(s"Unsubscribe Client: $clientId")
      clientActors -= clientId
    // called if a client runs the Adapter Process (Button)
    case RunAdapter(user) =>
      log.info(s"called runAdapter: $user")
      if (isRunning) // this should not happen as the button is disabled, if running
        log.warning("The adapter is running already!")
      else {
        log.info(s"run Adapter: $sender")
        runAdapter(user)
      }
    // there is one message that is not handled (KeepAliveMsg)
    case other =>
      log.info(s"unexpected message: $other")
  }

  // the process fakes some long taking tasks that logs its progress
  private def runAdapter(user: String) = {
    logService = LogService("Demo Adapter Process", user)
    isRunning = true
    Future {
      sendToSubscriber(logService.startLogging())
      for (i <- 0 to 10) {
        Thread.sleep(750)
        val ll = Random.shuffle(List(DEBUG, DEBUG, INFO, INFO, INFO, WARN, WARN, ERROR)).head
        sendToSubscriber(logService.log(ll, s"Adapter Process $ll: $i"))
      }
      sendToSubscriber(logService.stopLogging())
      isRunning = false
      sendToSubscriber(RunFinished(logService.logReport))
    }
  }

  private def sendToSubscriber(logEntry: LogEntry): Unit =
    sendToSubscriber(LogEntryMsg(logEntry))

  // sends an AdapterMsg to all subscribed clients
  private def sendToSubscriber(adapterMsg: AdapterMsg): Unit =
    clientActors.values
      .foreach(_ ! adapterMsg)
}

object AdapterActor {

  case class SubscribeAdapter(clientId: String, wsActor: ActorRef)

  case class UnSubscribeAdapter(clientId: String)

}
