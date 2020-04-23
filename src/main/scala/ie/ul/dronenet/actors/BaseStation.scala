package ie.ul.dronenet.actors

import java.io.{BufferedWriter, File, FileWriter}

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
import scala.util.{Failure, Success}


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

  case class ExecuteMission(replyTo: ActorRef[MissionResponse], origin: (Double, Double), dest: (Double, Double), weight: Double, distance: Double) extends Command
  case class MissionResponse(success: Boolean) extends Response

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

      case ExecuteMission(replyTo, origin, dest, weight, distance) => {
        // Get Registered Drone Details
        val droneFutures: List[Future[Drone.DetailsResponse]] = registeredDrones.toList.map(drone => {
          val f: Future[Drone.DetailsResponse] = drone.ask(ref => Drone.GetDetails(ref))
          f
        })

        Future.sequence(droneFutures).onComplete {
          case Success(details) => {
            context.log.info(s"\n\n\nRegistered drone details: ${details.toString()}\n\n")
            // Form JSON and execute MiniZinc Model
            val mapped = details.map(res => Tuple3(res.details._1, res.details._2, res.details._3))
            val tempFile = writeFile(mapped)
          }
          case Failure(exception) => context.log.error(exception.getMessage)
        }


        Behaviors.same
      }
    }
  }
  def writeFile(details: List[(String, Double, Double)]): File = {
    val file = File.createTempFile("drones_", ".json")
    val bw = new BufferedWriter(new FileWriter(file))
    var str: String = "{ \"n\":" + details.size + ", \"drone_attr\": [{\"e\": \"Range\"}, {\"e\": \"Capacity\"}], \"drones\": ["
    bw.write(str)
    details.filter(_ != details.last)
          .foreach(d => {
            str = s"[${d._2}, ${d._3}],"
            bw.write(str)
          })
    str = s"[${details.last._2}, ${details.last._3}]]}"
    bw.write(str) // Write Last in list WITHOUT comma to form correct JSON
    bw.close()
    context.log.info(s"Temporary File Created @ ${file.getAbsolutePath}")
    file
  }
}