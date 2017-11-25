package client

import com.thoughtworks.binding.Binding
import org.scalajs.dom.raw.HTMLElement

import scala.language.implicitConversions

trait IncidentImplicits {
  implicit def makeIntellijHappy(x: scala.xml.Elem): Binding[HTMLElement] = ???

}
