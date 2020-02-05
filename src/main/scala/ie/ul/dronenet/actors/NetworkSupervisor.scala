package ie.ul.dronenet.actors

import akka.actor.typed.{Behavior, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object NetworkSupervisor {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new NetworkSupervisor(context))
}

class NetworkSupervisor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println(s"${context.self.path} has started")

  override def onMessage(msg: String): Behavior[String] = Behaviors.empty
}
