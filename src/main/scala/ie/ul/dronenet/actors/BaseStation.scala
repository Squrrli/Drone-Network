package ie.ul.dronenet.actors

import akka.actor.{Actor, ActorLogging, Props}

object BaseStation {
  def props(id: Int, name: String): Props = Props(new BaseStation(id: Int, name: String))

  /* Actor Messages */
  final case object Ping

  final case class RequestCapacity(reqId: Long)
  final case class RespondCapacity(reqId: Long, capacity: Int)

  final case class RequestDrone(reqId: Long)
}

class BaseStation(id: Int, name: String) extends Actor with ActorLogging {
  import BaseStation._

  override def receive: Receive = {
    case Ping => log.info(s"Pinging Drone id:${id}")
    case RequestCapacity => {}
    case RequestDrone => {}
  }
}