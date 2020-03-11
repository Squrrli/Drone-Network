package ie.ul.dronenet.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import ie.ul.dronenet.actors
import ie.ul.dronenet.actors.BaseManager.{BSManagerServiceKey, ListingResponse, WrappedDroneManagerMsg}

import scala.collection.mutable

object BaseManager {

  val BSManagerServiceKey: ServiceKey[Command] = ServiceKey[Command]("baseManagerService")

  sealed trait Command extends CborSerializable

  private case class ListingResponse(listing: Receptionist.Listing) extends Command
  case class WrappedDroneManagerMsg(msg: DroneManager.Command) extends Command

  def apply(managerId: String): Behavior[Command] = {
    Behaviors.setup[Command] { context => new BaseManager(context, managerId)
//      context.log.debug("BSManager started")
//      val listingAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse)
//
//      // Register with local Receptionist and Subscribe to relevant Listings
//      context.system.receptionist ! Receptionist.register(BSManagerServiceKey, context.self)
//      context.system.receptionist ! Receptionist.Subscribe(BaseStation.BaseStationServiceKey, listingAdapter)
//      context.system.receptionist ! Receptionist.Subscribe(DroneManager.DroneManagerServiceKey, listingAdapter)
//
//      // Start BaseStation
//      val baseStation = context.spawn(BaseStation(managerId, context.self), "BaseStation-" + managerId)
//
//      Behaviors.receiveMessagePartial[Command] {
//        case ListingResponse(BaseStation.BaseStationServiceKey.Listing(listings)) =>
//          listings.foreach(d => context.log.info("BaseStation Path received from Receptionist Listing: {}", d.path))
//          Behaviors.same
//
//        case ListingResponse(DroneManager.DroneManagerServiceKey.Listing(listings)) =>
//          listings.foreach(dm => context.log.info("DroneManager Path received from Receptionist Listing: {}", dm.path))
//          Behaviors.same
//
//        case wrapped: WrappedDroneManagerMsg =>
//          context.log.debug("----- WrappedDroneManagerMsg received -----")
//          wrapped.msg match {
//            case DroneManager.RequestBaseStation(reqId, drone) =>
//              context.log.debug(s"RequestBaseStation message received - reqId: $reqId")
//              baseStation ! BaseStation.BaseRequested(reqId, drone)
//            case _ =>
//              context.log.debug("Message from DroneManager of type: {}", wrapped.msg)
//          }
//          Behaviors.same
//
//      }
    }
  }
}

class BaseManager(context: ActorContext[BaseManager.Command], managerId: String) extends AbstractBehavior[BaseManager.Command](context) {
  context.log.debug("BSManager started")
  val listingAdapter: ActorRef[Receptionist.Listing] = context.messageAdapter[Receptionist.Listing](ListingResponse)

  // Register with local Receptionist and Subscribe to relevant Listings
  context.system.receptionist ! Receptionist.register(BSManagerServiceKey, context.self)
  context.system.receptionist ! Receptionist.Subscribe(BaseStation.BaseStationServiceKey, listingAdapter)
  context.system.receptionist ! Receptionist.Subscribe(DroneManager.DroneManagerServiceKey, listingAdapter)

  // Start BaseStation
  val baseStation: ActorRef[BaseStation.Command] = context.spawn(BaseStation(managerId, context.self), "BaseStation-" + managerId)

  var base_station_listing: mutable.Set[ActorRef[BaseStation.Command]] = mutable.Set.empty
  var drone_manager_listing: mutable.Set[ActorRef[DroneManager.Command]] = mutable.Set.empty

  override def onMessage(msg: BaseManager.Command): Behavior[BaseManager.Command] = {
        msg match {
          case ListingResponse(BaseStation.BaseStationServiceKey.Listing(listings)) =>
            base_station_listing ++= listings
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
        }
  }
}