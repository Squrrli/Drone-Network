 package ie.ul.dronenet.actors

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorSystem, Behavior, receptionist}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.ClusterEvent.{MemberEvent, MemberRemoved, MemberUp, ReachableMember, UnreachableMember}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import ie.ul.dronenet.actors
import ie.ul.dronenet.actors.ClusterListener.{MemberChange, ReachabilityChange}
import ie.ul.dronenet.actors.Drone.Command

 // TODO: Remove Classes of Actors that will not have multiple instances running on the same machine (i.e. Singleton and Drone)
 // Better practice to make these Objects as they will only be instantiated once

object Drone {
  val DroneServiceKey: ServiceKey[Command] = ServiceKey[Command]("droneService")

  val TypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("Drone")

  def initSharding(system: ActorSystem[_]): Unit =
    ClusterSharding(system).init(Entity(TypeKey) { entityContext =>
      Drone(entityContext.entityId)
    })

  /* Drone Commands */
  sealed trait Command

  final case object Ping extends Command
//  final case object Ping
//
//  final case class RequestMission(reqId: Long, actorRef: ActorRef)
//  final case class AssignMission(reqId: Long, missionDetails: String) // TODO: mission details defined in some kind of structure/ object
//
//  final case class RequestAvailability(reqId: Long)
//  final case class RespondAvailability(reqId: Long, boolean: Boolean)
//
//  final case class RequestCoordinates(reqId: Long)
//  final case class RespondCoordinates(reqId: Long, coords: (Double, Double, Double)) // TODO: Look into this config. vs. Telling another actor to change route
//
//  final case class LocateClosestBaseStation()
//  final case class GoToBaseStation(coords: (Double, Double), actorRef: ActorRef)
  def apply(droneId: String): Behavior[Command] =
    Behaviors.setup[Command] {
      context =>
        context.log.info(s"Drone $droneId started")
        // Register Drone with local Receptionist to allow drone be discovered from across the Cluster
        context.system.receptionist ! Receptionist.register(DroneServiceKey, context.self)
        running(context, droneId)
    }

  private def running(context: ActorContext[Command], droneId: String):Behavior[Command] =
    Behaviors.receiveMessage[Command] { message =>
      message match {
        case Ping => context.log.info("Pinged!")

      }
      Behaviors.same
    }
}