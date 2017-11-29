# Telegram Bot with Play Framework, Akka FSM, Scala.js, Binding.scala
**This is work in progress.**

This project is based on:
1. [Telegram Bot Demo with Scala/ Play](https://github.com/pme123/play-scala-telegrambot4s)
2. [Websockets with Play Framework, Scala.js, Binding.scala](https://github.com/pme123/play-wsocket-scalajs)

In this example I want to combine everything from the 2. project above and extend it with:

* Using my [Small framework to handle multiple conversations with a telegram bot.](https://github.com/pme123/play-akka-telegrambot4s)

# Business Case
We want to have an overview of incidents that are reported by caretakers.
 
We implemented 2 Conversations:

## 1. Create an incident
They will report an incident to a Telegram Bot with their mobile phones: 
1. select type of incident.
1. select level of incident (urgency).
1. add a textual description.
1. add optional photos.
1. send the incident.

![incident](https://user-images.githubusercontent.com/3437927/33361929-5ff44212-d4da-11e7-8fcc-657815a0b45c.gif)

## 2. Edit an incident
1. send an ident.
2. select an edit action.
3. do the change.

![incidentedit](https://user-images.githubusercontent.com/3437927/33364230-c419af08-d4e3-11e7-9308-2120ff6605fe.gif)
## Control Panel

A web-page shows all incidents - the newest on top. To see the attached images you open a detail view.
You can filter and sort the incidents (see images above).

Let's start with the simple parts:

# Shared model
The great thing about a **full-stack Scala app** is that we only have to define our domain model once for the server and the client.

## Client-Server Communication
Next to the model all that is needed is the JSON-un-/-marshalling. 
If we use ADTs (Algebraic Data Types - `sealed traits` in Scala) in combination with case classes this requires only a few lines of code.

* Here the Thanks goes to: [Play JSON Derived Codecs](https://github.com/julienrf/play-json-derived-codecs)

Here is an example how that looks: [SharedMessages](https://github.com/pme123/play-akka-telegrambot4s-incidents/blob/simple-example/shared/src/main/scala/shared/SharedMessages.scala)

## Handling dates
Dates are handled differently on JVM and JS. 
`scalajs-java-time` provides an implementation of `java.time`. 
This allows to have to work with Instants on both sides (Be aware not everything is supported).
```scala
case class Audit(user: String
                 , dateTime: Instant = Instant.now()
                )
```
The JSON marshalling is now not too hard:
```scala
implicit val localInstantReads: Reads[Instant] =
    (json: JsValue) => {
      json.validate[Long]
        .map { epochSecond =>
          Instant.ofEpochSecond(epochSecond)
        }
    }

implicit val localInstantWrites: Writes[Instant] =
    (instant: Instant) => JsNumber(instant.getEpochSecond)

```
# Client
Has its own [README](client/README.md)

# Server
The server part can be split into the following sub-chapters:

## User management
When you go to [http://localhost:9000](http://localhost:9000) a web-socket is opened to show you incoming incidents.

The web-sockets are managed with Akka Actors. The implementation was taken from 
the **[Lightbend's Websocket example](https://github.com/playframework/play-scala-websocket-example)** 
and adjusted to fit my needs.

See [HomeController](https://github.com/pme123/play-akka-telegrambot4s-incidents/blob/simple-example/server/app/controllers/HomeController.scala)

## Chat management
This is now handled by my [small framework](https://github.com/pme123/play-akka-telegrambot4s). 
See the documentation there.

## Incident Conversation
Let's have a look now on the interesting part.
Always start with a description of your conversation;)
```
/**
  * report an incident with an IncidentType, a description and optional images.
  *
  *     [Idle]  <-------------
  *       v                  |
  *   [SelectIncidentType]   |
  *       v                  |
  *   [SelectIncidentLevel]  |
  *       v                  |
  *    [AddDescription]      |
  *       v                  |
  *   [AddAdditionalInfo] <--|
  *       v                  |
  *       --------------------
  */
```

Let's go through all states.
### Idle

```scala
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
```
  
### SelectIncidentType

```scala
  // first step after selecting IncidentType.
  when(SelectIncidentType) {
    case Event(Command(msg, callbackData: Option[String]), _) =>
      // now we check the callback data
      callbackData match {
        case Some(data) =>
          // ask the user for a description, as it is a text input no markup is needed. 
          bot.sendMessage(msg, "Add a description:")
          // when we go to the next step we add the IncidentType to the FSM.
          goto(SelectIncidentType) using IncidentTypeData(IncidentType.from(data))
        case None =>
          // when the user does not press a button - remind the user what we need
          bot.sendMessage(msg, "First you have to select the incident type!"
            , Some(incidentSelector))
          // and stay where we are
          stay()
      }
  }
```

### SelectIncidentLevel
Analog SelectIncidentType

### AddDescription
```scala
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
```

### AddAdditionalInfo
```scala
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
```
Here the whole class: 
[`IncidentConversation`](https://github.com/pme123/play-akka-telegrambot4s-incidents/blob/simple-example/server/app/bots/IncidentConversation.scala)

The 2. conversation you find here: 
[`EditIncidentConversation`](https://github.com/pme123/play-akka-telegrambot4s-incidents/blob/simple-example/server/app/bots/EditIncidentConversation.scala)

# Run the application
```shell
$ sbt
> run
```
open [http://localhost:9000](http://localhost:9000) in a browser.
