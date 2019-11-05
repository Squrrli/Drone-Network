package ie.ul.dronenet.actors

import akka.actor.{Actor, Props}

object BaseStation {
  def props(): Props = Props(new BaseStation)
}

class BaseStation extends Actor {
  override def receive: Receive = Actor.emptyBehavior
}