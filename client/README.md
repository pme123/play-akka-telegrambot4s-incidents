# Scala.js & Binding.scala Client

I want to point out here some interesting points, when working with Scala.js and Binding.scala.

## Composition



Once again it's build with [Binding.scala](https://github.com/ThoughtWorksInc/Binding.scala)

Thanks to that, the code fits in 150 lines: [IncidentClient](https://github.com/pme123/play-akka-telegrambot4s-incidents/blob/simple-example/client/src/main/scala/client/IncidentClient.scala)

It is more or less HTML-snippets that contain dynamic content provided by Binding.scala:
* `Vars[Incident]` hosts all reported incidents.
* `Var[Option[Incident]]` is set if a User wants to see a detail of an incident.

If you have troubles understanding it, please check out [Binding.scala-Google-Maps](https://github.com/pme123/Binding.scala-Google-Maps), where I explained all the details.

