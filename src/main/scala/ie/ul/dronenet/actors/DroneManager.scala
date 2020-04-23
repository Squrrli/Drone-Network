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

  def apply(droneName: String, dType: String, range: Double, maxWeight: Double): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
        val drone = context.spawn(Drone(droneName, dType, range, maxWeight, context.self), droneName)
        val msgAdapterListingResponse = context.messageAdapter[Receptionist.Listing](ListingResponse)
        val router = context.spawn(routerGroup.withRoundRobinRouting(), "BaseStationManager-group")

        var noInitialBaseStation: Boolean = true
        var listingCtr: Int = 0

        // Register with local Receptionist and Subscribe to Listings of relevant ServiceKeys
        context.system.receptionist ! Receptionist.register(DroneManagerServiceKey, context.self)
        context.system.receptionist ! Receptionist.Subscribe(Drone.DroneServiceKey, msgAdapterListingResponse)
        context.system.receptionist ! Receptionist.Subscribe(BaseManager.BSManagerServiceKey, msgAdapterListingResponse)

        Behaviors.receiveMessage[Command] {
          case DroneReady =>
            context.log.debug("Drone ready message received")
//            context.system.receptionist ! Receptionist.Find(BaseManager.BSManagerServiceKey, msgAdapterListingResponse)
            Behaviors.same

          case ListingResponse(Drone.DroneServiceKey.Listing(listings)) =>
            context.log.debug("Drone listings is empty: {}", listings.isEmpty)
            listings.foreach(d => context.log.info("ListingResponse(DroneServiceKey): {}",d.hashCode()))
            Behaviors.same

          case ListingResponse(BaseManager.BSManagerServiceKey.Listing(listings)) =>
            context.log.debug("{} - BaseManager listings is empty: {}", listingCtr, listings.isEmpty)
            // get initial base station once listing has been populated
            if(noInitialBaseStation && listingCtr > 0) {
              noInitialBaseStation = false
              listings.foreach( _ => router ! BaseManager.WrappedDroneManagerMsg(RequestBaseStation(0,drone))              )
            }
            listingCtr = 1
            Behaviors.same

          case RequestBaseStation(reqId, drone) => // TODO: Restructure to ensure that Listing Set is always updated before requesting a BaseStation
//            context.log.debug(s"RequestBaseStation: reqId{$reqId}")
//            router ! BaseManager.WrappedDroneManagerMsg(RequestBaseStation(reqId, drone))
//            context.log.debug(" --- post router ! WrappedDroneManagerMsg()")
            Behaviors.same
        }
    }
  }
}

class DroneManager(context: ActorContext[DroneManager.Command], dType: String, range: Double, maxWeight: Double) extends AbstractBehavior[DroneManager.Command](context) {
  override def onMessage(msg: DroneManager.Command): Behavior[DroneManager.Command] = ???
}