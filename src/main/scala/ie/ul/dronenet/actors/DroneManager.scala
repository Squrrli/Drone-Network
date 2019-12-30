package ie.ul.dronenet.actors

import akka.actor.{Actor, ActorLogging, Props}

object DroneManager {
  def props(): Props = Props(new DroneManager)

  /* Actor Messages */
  final case object Ping
}

class DroneManager extends Actor with ActorLogging{
  import DroneManager._

  override def receive: Receive = {
    case Ping => log.info("Pinged")
  }
}
