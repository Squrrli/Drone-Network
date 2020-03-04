package ie.ul.dronenet.actors

import akka.actor.{ActorSystem, typed}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

sealed trait Command extends CborSerializable

/**
 * Class to handle communication between frontend and Drone Network
 * Singleton actor through with all socket communication takes place.
 */
class IOSocket(context: ActorContext[Command]) extends AbstractBehavior[Command](context) {
  implicit val system: typed.ActorSystem[Nothing] = context.system

  override def onMessage(msg: Command): Behavior[Command] = ???
}
