package ie.ul.dronenet.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object BaseManager {
  def props(): Props = Props(new BaseManager)

  /* Actor Messages */
  final case object Ping

  final case class RequestDroneFromDroneManager(reqId: Long)
  final case class RespondDroneToBase(reqId: Long, actorRef: ActorRef)
}

class BaseManager extends Actor with ActorLogging{
  import BaseManager._

  override def  receive: Receive = {
    case RequestDroneFromDroneManager => _
    case RespondDroneToBase => _
    case Ping => log.info("PING")
  }
}