package ie.ul.dronenet.actors

import akka.actor.{Actor, Props}

object BaseManager {
  def props(): Props = Props(new BaseManager)
}

class BaseManager extends Actor {
  override def receive: Receive = Actor.emptyBehavior
}