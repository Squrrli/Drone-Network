package ie.ul.dronenet.actors

import akka.actor.{Scheduler, typed}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.scaladsl.AskPattern._
import akka.pattern.AskableActorRef
import akka.util.Timeout

import scala.concurrent.duration._
import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object BaseManager {

  val BSManagerServiceKey: ServiceKey[Command] = ServiceKey[Command]("baseManagerService")

  sealed trait Command extends CborSerializable
  sealed trait Response extends CborSerializable

  private case class ListingResponse(listing: Receptionist.Listing) extends Command
  case class WrappedDroneManagerMsg(msg: DroneManager.Command) extends Command
  case class SetIOSocket(ref: ActorRef[IOSocket.Command]) extends Command

  case class StationsResponse(stations: mutable.Set[ActorRef[BaseStation.Command]]) extends Command
  case class GetAllStations(replyTo: ActorRef[AllDetailsResponse]) extends Command
  case class AllDetailsResponse(stations: List[(String, Double, Double)]) extends Response
  case class BaseDetailsResAdapter(responses: List[BaseStation.Response]) extends Command

  case object ResFailed extends Command

  def apply(managerId: String, capacity: Double, latlng: Seq[Double]): Behavior[Command] = {
    Behaviors.setup[Command](context => new BaseManager(context, managerId, capacity, latlng))
  }
}

class BaseManager(context: ActorContext[BaseManager.Command], baseName: String, capacity: Double, latlng: Seq[Double])
  extends AbstractBehavior[BaseManager.Command](context) {
  import BaseManager._

  context.log.debug("{} started @ {}...", baseName, latlng)
  val listingAdapter: ActorRef[Receptionist.Listing] = context.messageAdapter[Receptionist.Listing](ListingResponse)

  // Register with local Receptionist and Subscribe to relevant Listings
  context.system.receptionist ! Receptionist.register(BSManagerServiceKey, context.self)
  context.system.receptionist ! Receptionist.Subscribe(BaseStation.BaseStationServiceKey, listingAdapter)
  context.system.receptionist ! Receptionist.Subscribe(DroneManager.DroneManagerServiceKey, listingAdapter)

  // Start BaseStation
  val baseStation: ActorRef[BaseStation.Command] = context.spawn(BaseStation(baseName, context.self, capacity, latlng), baseName)
  // val required for futures
  implicit val timeout: Timeout = 3.seconds
  implicit val ec: ExecutionContextExecutor = context.executionContext
  implicit val scheduler: typed.Scheduler = context.system.scheduler

  var ioSocket: Option[ActorRef[IOSocket.Command]] = None
  var base_station_listing: Set[ActorRef[BaseStation.Command]] = Set.empty
  var drone_manager_listing: Set[ActorRef[DroneManager.Command]] = Set.empty

  override def onMessage(msg: BaseManager.Command): Behavior[BaseManager.Command] = {
        msg match {
          case ListingResponse(BaseStation.BaseStationServiceKey.Listing(listings)) =>
            base_station_listing = listings
            // Send IOSocket ActorRef to allow for updating frontend
            Behaviors.same

          case ListingResponse(DroneManager.DroneManagerServiceKey.Listing(listings)) =>
            drone_manager_listing = listings
//            baseStation ! RemoveDeadDrones(listing) TODO: Implement Removal of dead drones from BaseStation list
            Behaviors.same

          case wrapped: WrappedDroneManagerMsg =>
            context.log.debug("----- WrappedDroneManagerMsg received -----")
            wrapped.msg match {
              case DroneManager.RequestBaseStation(reqId, drone) =>
                context.log.debug(s"RequestBaseStation message received - reqId: $reqId")
                baseStation ! BaseStation.BaseRequested(reqId, drone)
              case _ =>
                context.log.debug("Message from DroneManager of type: {}", wrapped.msg)
            }
            Behaviors.same

          case SetIOSocket(socket) =>
            ioSocket = Some(socket)
            socket ! IOSocket.SetBaseManagerRef(context.self)
            Behaviors.same

          case GetAllStations(replyTo) => {
            val futures: List[Future[BaseStation.DetailsResponse]] = base_station_listing.toList.map( station => {
              val f: Future[BaseStation.Response] = station.ask(ref => BaseStation.GetBaseDetails(ref))
              f.mapTo[BaseStation.DetailsResponse]
            })

            Future.sequence(futures).onComplete {
              case Success(responses) =>
                val mapped = responses.map(res => Tuple3(res.details._1, res.details._2, res.details._3))
                context.log.info("replying to IOSocket")
                context.log.info("\n\n\n" + mapped + "\n\n")
                replyTo ! AllDetailsResponse(mapped)
              case Failure(_) => context.log.error("something went wrong while getting futures")
            }
            Behaviors.same
          }
        }
  }
}