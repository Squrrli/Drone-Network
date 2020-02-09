//package ie.ul.dronenet.actors
//
//import akka.actor.typed.Behavior
//import akka.actor.typed.receptionist.Receptionist
//import akka.actor.{Actor, ActorLogging, ActorRef, Props}
//
//object BaseManager {
//  def apply(): Behavior[Receptionist.Listing]
//
//  /* Actor Messages */
//  final case object Ping
//
//  final case class RequestDroneFromDroneManager(reqId: Long)
//  final case class RespondDroneToBase(reqId: Long, actorRef: ActorRef)
//}
