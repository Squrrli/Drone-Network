package ie.ul.dronenet.actors

import akka.actor.typed.{ActorRef, Behavior, receptionist}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, GroupRouter, Routers}
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import ie.ul.dronenet.actors.CborSerializable

import scala.collection.mutable

object DroneManager {
  val DroneManagerServiceKey: ServiceKey[Command] = ServiceKey[Command]("droneManagerService")

  val routerGroup: GroupRouter[BaseManager.Command] = Routers.group(BaseManager.BSManagerServiceKey)

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes(
    Array(new JsonSubTypes.Type(value = classOf[RequestBaseStation]))
  )
  sealed trait Command extends CborSerializable

  final case object DroneReady extends Command
  private case class ListingResponse(listing: Receptionist.Listing) extends Command

  final case class RequestBaseStation(reqId: Long, drone: ActorRef[Drone.Command]) extends Command

  def apply(managerId: String): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
        val drone = context.spawn(Drone(managerId, context.self), "drone-" + managerId)
        val msgAdapterListingResponse = context.messageAdapter[Receptionist.Listing](ListingResponse)
        val routerGroup: GroupRouter[BaseManager.Command] = Routers.group(BaseManager.BSManagerServiceKey)
        val router = context.spawn(routerGroup.withRoundRobinRouting(), "baseStationManager-group")

        var managerNotReady: Boolean = true

        // Register with local Receptionist and Subscribe to Listings of relevant ServiceKeys
        context.system.receptionist ! Receptionist.register(DroneManagerServiceKey, context.self)
        context.system.receptionist ! Receptionist.Subscribe(Drone.DroneServiceKey, msgAdapterListingResponse)
//        context.system.receptionist ! Receptionist.Subscribe(BaseManager.BSManagerServiceKey, msgAdapterListingResponse)

        Behaviors.receiveMessage[Command] {
          case DroneReady =>
            context.system.receptionist ! Receptionist.Find(BaseManager.BSManagerServiceKey, msgAdapterListingResponse)
            Behaviors.same

          case receptionistListing: ListingResponse =>
            receptionistListing.listing match {
              case Drone.DroneServiceKey.Listing(listings) =>
                context.log.debug("----- Drone.DroneServiceKey.Listing received -----")
              case BaseManager.BSManagerServiceKey.Listing(listings) =>
                context.log.debug("----- Drone.BSManagerServiceKey.Listing received -----")
                listings.foreach(bm => context.log.debug("received BaseManager ActorRef from Receptionist: {}", bm.hashCode()))
                if(managerNotReady) {
                  drone ! Drone.ManagerReady
                  managerNotReady = false
                }
            }
            Behaviors.same

          case RequestBaseStation(reqId, drone) => // TODO: Restructure to ensure that Listing Set is always updated before requesting a BaseStation
            context.log.debug(s"RequestBaseStation: reqId{$reqId}")
            router ! BaseManager.WrappedDroneManagerMsg(RequestBaseStation(reqId, drone))
            context.log.debug(" --- post router ! WrappedDroneManagerMsg()")
            Behaviors.same
        }
    }
  }
}

class DroneManager(context: ActorContext[DroneManager.Command]) extends AbstractBehavior[DroneManager.Command](context) {
  override def onMessage(msg: DroneManager.Command): Behavior[DroneManager.Command] = ???
}