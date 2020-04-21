 package ie.ul.dronenet.actors

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, receptionist}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import ie.ul.dronenet.actors.DroneManager.RequestBaseStation

object Drone {
  val DroneServiceKey: ServiceKey[Command] = ServiceKey[Command]("droneService")
  val TypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("Drone")

  var currentBaseStation: Option[ActorRef[BaseStation.Command]] = None
  var localManager: Option[ActorRef[DroneManager.Command]] = None

  /* Drone Commands */
  sealed trait Command extends CborSerializable
  sealed trait Response extends CborSerializable
  final case object Ping extends Command
  final case object ManagerReady extends Command
  final case class RegisterBaseStation(replyTo: ActorRef[Response]) extends Command

  final case object RegisterResponse extends Response

  def apply(droneId: String, dType: String, range: Double, maxWeight: Double, droneManager: ActorRef[DroneManager.Command]): Behavior[Command] =
    Behaviors.setup[Command] {
      context =>  new Drone(context, droneId, dType, range, maxWeight, droneManager)
    }
}

 class Drone(context: ActorContext[Drone.Command], droneId: String, dType: String, range: Double, maxWeight: Double, droneManager: ActorRef[DroneManager.Command])
   extends AbstractBehavior[Drone.Command](context) {
   import Drone._

   var registeredToBase = false

   context.log.info(s"Drone $droneId started")
   // Register Drone with local Receptionist to allow drone be discovered from across the Cluster
   context.system.receptionist ! Receptionist.register(DroneServiceKey, context.self)
   droneManager ! DroneManager.DroneReady  // inform Manager that this Drone is ready


   override def onMessage(msg: Drone.Command): Behavior[Drone.Command] = {
     msg match {
       case Ping =>
         context.log.info("Pinged!")
         Behaviors.same
       case RegisterBaseStation(baseStation) =>
         context.log.info("BS asking to register...")
         if(!registeredToBase)
           baseStation ! RegisterResponse
         Behaviors.same
//
//       case ManagerReady =>
//         context.log.info("-------------- Requesting BaseStation --------------")
//         droneManager ! RequestBaseStation(0, context.self)
//         Behaviors.same
     }
   }
 }