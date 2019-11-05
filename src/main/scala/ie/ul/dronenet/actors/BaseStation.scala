package ie.ul.dronenet.actors

import akka.actor.{Actor, ActorLogging, Props}

object BaseStation {
  def props(id: Int, name: String): Props = Props(new BaseStation(id: Int, name: String))

  final case class RequestCapacity(reqId: Long)
  final case class RespondCapacity(reqId: Long, capacity: Int)

  final case class RequestDrone(reqId: Long)
}

class BaseStation(id: Int, name: String) extends Actor with ActorLogging {
  import BaseStation._

  override def receive: Receive = {
    case RequestCapacity => {}
    case RequestDrone => {}
  }
}