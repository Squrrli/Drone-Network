package ie.ul.dronenet.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.{Actor, ActorLogging, Props}
import ie.ul.dronenet.actors.BaseStation.Command
import ie.ul.dronenet.actors.Drone.DroneServiceKey

object BaseManager {

  val BSManagerServiceKey: ServiceKey[Command] = ServiceKey[Command]("baseManagerService")

  sealed trait Command
  private case class ListingResponse(listing: Receptionist.Listing) extends Command

  def apply(managerId: String, localBase: ActorRef[BaseStation.Command]): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
      context.log.debug("BSManager started")
      val messageAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse)

      context.system.receptionist ! Receptionist.register(BSManagerServiceKey, context.self)
      // Subscribe to relevant Receptionist Listings
      context.system.receptionist ! Receptionist.Subscribe(BaseStation.BaseStationServiceKey, messageAdapter)
      context.system.receptionist ! Receptionist.Subscribe(DroneManager.DroneManagerServiceKey, messageAdapter)

      Behaviors.receiveMessagePartial[Command] {
        case ListingResponse(BaseStation.BaseStationServiceKey.Listing(listings)) =>
          listings.foreach(d => context.log.info("BaseStation Path received from Receptionist Listing: {}", d.path))
          Behaviors.same
        case ListingResponse(DroneManager.DroneManagerServiceKey.Listing(listings)) =>
          listings.foreach(dm => context.log.info("DroneManager Path received from Receptionist Listing: {}", dm.path))
          Behaviors.same
      }
    }
  }
}