 package ie.ul.dronenet.actors

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, receptionist}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import com.fasterxml.jackson.databind.InjectableValues.Std
import ie.ul.dronenet.actors.DroneManager.RequestBaseStation

import scala.io.StdIn

 // TODO: Remove Classes of Actors that will not have multiple instances running on the same machine (i.e. Singleton and Drone)
 // Better practice to make these Objects as they will only be instantiated once

object Drone {
  val DroneServiceKey: ServiceKey[Command] = ServiceKey[Command]("droneService")
  val TypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("Drone")

  var currentBaseStation: Option[ActorRef[BaseStation.Command]] = None
  var localManager: Option[ActorRef[DroneManager.Command]] = None

  // TODO: continue using without sharding, see if work
  // Possible that sharding is only required to split one Node across multiple machines (Single BaseStation served by multiple machines"
//  def initSharding(system: ActorSystem[_]): Unit =
//    ClusterSharding(system).init(Entity(TypeKey) { entityContext =>
//      Drone(entityContext.entityId)
//    })

  /* Drone Commands */
  sealed trait Command extends CborSerializable

  final case object Ping extends Command
  final case object ManagerReady extends Command
  final case class RegisterBaseStation(reqId: Long, baseStation: ActorRef[BaseStation.Command]) extends Command

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

  /**
   * Override of apply() method for instantiating Actor
   * @param droneId String ID of this Drone
   * @param dType type of drone e.g. Quad, Hex, Fixed
   * @param range max flight range of drone
   * @param maxWeight max carry weight range of drone
   * @param droneManager DroneManager Actor that facilitates actor discovery across the cluster for this Drone actor
   * @return Behavior[Drone.Command]
   */
  def apply(droneId: String, dType: String, range: Double, maxWeight: Double, droneManager: ActorRef[DroneManager.Command]): Behavior[Command] =
    Behaviors.setup[Command] {
      context =>
        context.log.info(s"Drone $droneId started")
        // Register Drone with local Receptionist to allow drone be discovered from across the Cluster
        context.system.receptionist ! Receptionist.register(DroneServiceKey, context.self)
        droneManager ! DroneManager.DroneReady  // inform Manager that this Drone is ready

        running(context, droneId, droneManager)
    }

  private def running(context: ActorContext[Command], droneId: String, droneManager: ActorRef[DroneManager.Command]):Behavior[Command] =
    Behaviors.receiveMessage[Command] {
      case Ping =>
        context.log.info("Pinged!")
        Behaviors.same
      case RegisterBaseStation(reqId, baseStation) =>
        context.log.info("Response from BaseStation, registering {} as current, reqId: {}", baseStation, reqId)
        currentBaseStation = Some(baseStation)
        Behaviors.same
      case ManagerReady =>
        context.log.info("-------------- Requesting BaseStation --------------")
        droneManager ! RequestBaseStation(0, context.self)
        Behaviors.same
    }
}