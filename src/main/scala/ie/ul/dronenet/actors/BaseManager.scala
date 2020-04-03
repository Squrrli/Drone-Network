package ie.ul.dronenet.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object BaseManager {

  val BSManagerServiceKey: ServiceKey[Command] = ServiceKey[Command]("baseManagerService")

  sealed trait Command extends CborSerializable

  private case class ListingResponse(listing: Receptionist.Listing) extends Command
  case class WrappedDroneManagerMsg(msg: DroneManager.Command) extends Command
  case class SetIOSocket(ref: ActorRef[IOSocket.Command]) extends Command
  case class StationsResponse(stations: mutable.Set[ActorRef[BaseStation.Command]]) extends Command

  case class GetAllStations(sender: ActorRef[Response]) extends Command
  case class Response(stations: Set[(String, Float, Float)])

  def apply(managerId: String): Behavior[Command] = {
    Behaviors.setup[Command](context => new BaseManager(context, managerId))
  }
}

class BaseManager(context: ActorContext[BaseManager.Command], managerId: String) extends AbstractBehavior[BaseManager.Command](context) {
  import BaseManager._

  context.log.debug("{} started...", managerId)
  val listingAdapter: ActorRef[Receptionist.Listing] = context.messageAdapter[Receptionist.Listing](ListingResponse)

  // Register with local Receptionist and Subscribe to relevant Listings
  context.system.receptionist ! Receptionist.register(BSManagerServiceKey, context.self)
  context.system.receptionist ! Receptionist.Subscribe(BaseStation.BaseStationServiceKey, listingAdapter)
  context.system.receptionist ! Receptionist.Subscribe(DroneManager.DroneManagerServiceKey, listingAdapter)

  // Start BaseStation
  val baseStation: ActorRef[BaseStation.Command] = context.spawn(BaseStation(managerId, context.self), "BaseStation-" + managerId)
  // val required for futures
  implicit val timeout: Timeout = 3.seconds
  implicit val ec: ExecutionContextExecutor = context.executionContext

  var ioSocket: Option[ActorRef[IOSocket.Command]] = None
  var base_station_listing: mutable.Set[ActorRef[BaseStation.Command]] = mutable.Set.empty
  var drone_manager_listing: mutable.Set[ActorRef[DroneManager.Command]] = mutable.Set.empty

  override def onMessage(msg: BaseManager.Command): Behavior[BaseManager.Command] = {
        msg match {
          case ListingResponse(BaseStation.BaseStationServiceKey.Listing(listings)) =>
            base_station_listing ++= listings
            // Send IOSocket ActorRef to allow for updating frontend
            Behaviors.same

          case ListingResponse(DroneManager.DroneManagerServiceKey.Listing(listings)) =>
            drone_manager_listing ++= listings
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

          case GetAllStations(sender) =>
//            sender ! Response(base_station_listing)
            val stationFutures = Future.sequence( {
              for(station <- base_station_listing) {
                Future {
                  context.ask(station, GetStationDetails) {
                    case Success(value) => Future.successful(Adapter(value)) // do stuff and mark future as successful
                    case Failure(ex) => _ // Do nothing (?) no response from the drone
                  }
                }
              }
            })

            stationFutures.onComplete( {
              case Success(value) => sender ! Response(value)
            })

            Behaviors.same
        }
  }
}