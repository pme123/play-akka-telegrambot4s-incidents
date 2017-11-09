# Telegram Bot with Play Framework, Akka FSM, Scala.js, Binding.scala

This project is based on:
* [Websockets with Play Framework, Scala.js, Binding.scala](https://github.com/pme123/play-wsocket-scalajs)

In this example I want to combine everything from the project above and extend it with:

* Using my [Small framework to handle multiple conversations with a telegram bot.](https://github.com/pme123/play-akka-telegrambot4s)

Let's start with the simple parts:
# Shared model
The great thing about a **full-stack Scala app** is that we only have to define our domain model once for the server and the client.

Next to the model all that is needed is the JSON-un-/-marshalling. Thanks to the `julienrf.json.derived` library this involves only a few lines of code.

Here is the whole class: 

This is an example application showing how you can integrate a Play project with a Scala.js, Binding.scala project - using Web Sockets.

It's about an automatic process that can be started manually (button). But it should run only once at a time.

So only one actor (IncidentActor) will run the process and make sure, that the process is only run once at a time.

The web-sockets are created according to the example.

Each client sees the LogEntries of the last 'Incident process' (LogReport) - or if the process is running - each LogEntry right away.

The Binding.scala takes care of:
* show the LogEntries
* disable the 'Run Incident' button
* show the last LogLevel of the Incident Process

## Run the application
```shell
$ sbt
> run
$ open http://localhost:9000
```
