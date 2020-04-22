package ie.ul.dronenet.actors

import akka.actor.typed
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, receptionist}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}

import scala.concurrent.{ExecutionContextExecutor, Future}
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import scala.collection.mutable
import scala.concurrent.duration._


object BaseStation {
  val BaseStationServiceKey: ServiceKey[Command] = ServiceKey[Command]("baseService")

  sealed trait Command extends CborSerializable
  sealed trait Response extends CborSerializable

  final case object Ping extends Command

  final case class BaseRequested(reqId: Long, drone: ActorRef[Drone.Command], initial: Boolean = true) extends Command
  case class GetBaseDetails(replyTo: ActorRef[Response]) extends Command
  case class DetailsResponse(details: (String, Double, Double)) extends Response

  case class RemoveDeadDrones(listing: Set[ActorRef[DroneManager.Command]]) // TODO: Finish implementing removal of drones

  case class RegisterDrone(drone: ActorRef[Drone.Command]) extends Command
  case class UnregisterDrone(drone: ActorRef[Drone.Command]) extends Command

  def apply(baseId: String, manager: ActorRef[BaseManager.Command], capacity: Double, latlng: Seq[Double]): Behavior[Command] =
    Behaviors.setup[Command] {
      context => new BaseStation(context, baseId, manager, capacity, latlng)
    }
}

class BaseStation(context: ActorContext[BaseStation.Command], baseId: String, manager: ActorRef[BaseManager.Command], capacity: Double, latlng: Seq[Double])
  extends AbstractBehavior[BaseStation.Command](context) {
  import BaseStation._

  context.log.info(s"Drone $baseId started")
  // Register Drone with local Receptionist to allow drone be discovered from across the Cluster
  context.system.receptionist ! Receptionist.register(BaseStationServiceKey, context.self)

  implicit val timeout: Timeout = 3.seconds
  implicit val ec: ExecutionContextExecutor = context.executionContext
  implicit val scheduler: typed.Scheduler = context.system.scheduler

  private val registeredDrones: mutable.Set[ActorRef[Drone.Command]] = mutable.Set()

  override def onMessage(msg: BaseStation.Command): Behavior[BaseStation.Command] = {
    msg match {
      case BaseRequested(reqId, drone, initial) => // TODO: refactor to not need initial param
        context.log.info("BaseRequested by {}, reqId: {}", drone, reqId)
        context.log.info("\nCAPACITY: {}, registered: {}\n", capacity, registeredDrones.size)

//        if(registeredDrones.size < capacity) {
          val futureRegister: Future[Drone.Response] = drone.ask(ref => Drone.RegisterBaseStation(ref))
          futureRegister.map {
            case Drone.RegisterResponse =>
              context.log.debug(s"Register Drone ${drone.path} @ ${context.self.path}")
              registeredDrones.add(drone)
            case Drone.NoRegisterResponse =>
              context.log.debug(s"Drone not registered @ ${context.self.path}")
          }
//        }
        Behaviors.same

      case GetBaseDetails(replyTo) =>
        context.log.info("\n---------------- BaseDetailsRequested ---------------\n")
        val res: (String, Double, Double) = (baseId, latlng.head, latlng(1))
        replyTo ! DetailsResponse(res)
        Behaviors.same
    }
  }
}