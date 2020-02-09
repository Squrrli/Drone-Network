package ie.ul.dronenet.actors

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, receptionist}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}

import scala.collection.mutable


object BaseStation {
  val BaseStationServiceKey: ServiceKey[Command] = ServiceKey[Command]("baseService")
  val TypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("Base")

  val currentDrones: mutable.Set[ActorRef[Drone.Command]] = mutable.Set.empty[ActorRef[Drone.Command]]

  def initSharding(system: ActorSystem[_]): Unit =
    ClusterSharding(system).init(Entity(TypeKey) { entityContext =>
      BaseStation(entityContext.entityId)
    })

  /* Drone Commands */
  sealed trait Command

  final case object Ping extends Command
  case class RegisterDrone(drone: ActorRef[Drone.Command]) extends Command
  case class UnregisterDrone(drone: ActorRef[Drone.Command]) extends Command

  def apply(baseId: String): Behavior[Command] =
    Behaviors.setup[Command] {
      context =>
        context.log.info(s"Drone $baseId started")
        // Register Drone with local Receptionist to allow drone be discovered from across the Cluster
        context.system.receptionist ! Receptionist.register(BaseStationServiceKey, context.self)
        running(context, baseId)
    }

  private def running(context: ActorContext[Command], droneId: String):Behavior[Command] =
    Behaviors.receiveMessage[Command] { message =>
      message match {
        case Ping => context.log.info("Pinged!")
        case RegisterDrone(drone) =>
          currentDrones += drone
          context.log.debug("currentDrone: {}", currentDrones)
        case UnregisterDrone(drone) =>
          if (currentDrones(drone)) currentDrones -= drone
          context.log.debug("currentDrone: {}", currentDrones)
      }
      Behaviors.same
    }
}