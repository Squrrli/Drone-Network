package ie.ul.dronenet.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object BaseManager {
  def props(): Props = Props(new BaseManager)

//  final case class RequestDroneFromDroneManager(reqId: Long)
//  final case class RespondDroneToBase(reqId: Long, actorRef: ActorRef)
}

class BaseManager extends Actor with ActorLogging{
  override def receive: Receive = Actor.emptyBehavior
}