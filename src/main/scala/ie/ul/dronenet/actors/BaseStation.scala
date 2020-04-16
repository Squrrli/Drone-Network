package ie.ul.dronenet.actors

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, receptionist}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}

import scala.collection.mutable


object BaseStation {
  val BaseStationServiceKey: ServiceKey[Command] = ServiceKey[Command]("baseService")
  
  /* Drone Commands */
  sealed trait Command extends CborSerializable
  sealed trait Response extends CborSerializable

  final case object Ping extends Command

  final case class BaseRequested(reqId: Long, drone: ActorRef[Drone.Command], initial: Boolean = true) extends Command
  case class GetBaseDetails(replyTo: ActorRef[Response]) extends Command
  case class DetailsResponse(details: (String, Float, Float)) extends Response

  case class RegisterDrone(drone: ActorRef[Drone.Command]) extends Command
  case class UnregisterDrone(drone: ActorRef[Drone.Command]) extends Command

  def apply(baseId: String, manager: ActorRef[BaseManager.Command], capacity: Double, latlng: Seq[Double]): Behavior[Command] =
    Behaviors.setup[Command] {
      context =>
        context.log.info(s"Drone $baseId started")
        // Register Drone with local Receptionist to allow drone be discovered from across the Cluster
        context.system.receptionist ! Receptionist.register(BaseStationServiceKey, context.self)
        running(context, baseId)
    }

  private def running(context: ActorContext[Command], droneId: String):Behavior[Command] =
    Behaviors.receiveMessage[Command] {
        case Ping => context.log.info("Pinged!")
          Behaviors.same
        case BaseRequested(reqId, drone, initial) =>
          context.log.info("BaseRequested by {}, reqId: {}", drone, reqId)
          if(initial)
            // wait for manual drone acceptance
            context.log.info("Initial Drone BaseStation request - waiting for confirmation...\n (y)es / (n)o ?")
          else
            // autonomous selection
            context.log.info("Drone BaseStation request - determining if should respond")
          Behaviors.same
        case GetBaseDetails(replyTo) =>
          context.log.info("\n---------------- BaseDetailsRequested ---------------\n")
          val res: (String, Float, Float) = ("hello", 2, 2)
          replyTo ! DetailsResponse(res)
          Behaviors.same
    }
}