package ie.ul.dronenet.actors

import akka.actor.{Actor, Props}

object DroneManager {
  def props(): Props = Props(new DroneManager)
}

class DroneManager extends Actor {
  override def receive: Receive = Actor.emptyBehavior
}
