 package ie.ul.dronenet.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object Drone{
  def props(id: Long, droneType: Int): Props = Props(new Drone(id: Long, droneType: Int))

  /* Actor Messages */
  final case object Ping

  final case class RequestMission(reqId: Long, actorRef: ActorRef)
  final case class AssignMission(reqId: Long, missionDetails: String) // TODO: mission details defined in some kind of structure/ object

  final case class RequestAvailability(reqId: Long)
  final case class RespondAvailability(reqId: Long, boolean: Boolean)

  final case class RequestCoordinates(reqId: Long)
  final case class RespondCoordinates(reqId: Long, coords: (Double, Double, Double)) // TODO: Look into this config. vs. Telling another actor to change route

  final case class LocateClosestBaseStation()
  final case class GoToBaseStation(coords: (Double, Double), actorRef: ActorRef)
}

/**
 * Drone Actor Class
 * @param id Unique ID of the Drone - Alternative to ActorRef
 * @param droneType Enum type of the Drone e.g. Quad, FixedWing etc.
 */
class Drone(id: Long, droneType: Int) extends Actor with ActorLogging {
  import Drone._

  override def receive: Receive = {
    case Ping => log.info(s"Pinging Drone id:${id}")
    case AssignMission(reqId, missionDetails) => log.info(s"ReqId: ${reqId}, Mission: ${missionDetails}")
    case GoToBaseStation => log.info("go to station")
    case RequestAvailability => log.info("req availability")
    case RequestCoordinates => log.info("req coords")
  }
}