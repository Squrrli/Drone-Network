package ie.ul.dronenet.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.{Actor, ActorLogging, Props}
import ie.ul.dronenet.actors.Drone.DroneServiceKey

object DroneManager {

  val DroneManagerServiceKey: ServiceKey[Command] = ServiceKey[Command]("droneManagerService")

  sealed trait Command
  private case class ListingResponse(listing: Receptionist.Listing) extends Command

  def apply(managerId: String, localDrone: ActorRef[Drone.Command]): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
        val messageAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse)

        context.system.receptionist ! Receptionist.register(DroneManagerServiceKey, context.self)
      // Subscribe to relevant Receptionist Listings
        context.system.receptionist ! Receptionist.Subscribe(Drone.DroneServiceKey, messageAdapter)
        context.system.receptionist ! Receptionist.Subscribe(BaseManager.BSManagerServiceKey, messageAdapter)

        Behaviors.receiveMessagePartial[Command] {
          case ListingResponse(Drone.DroneServiceKey.Listing(listings)) =>
            listings.foreach(d => context.log.info("Drone Path received from Receptionist Listing: {}", d.path))
            Behaviors.same
          case ListingResponse(BaseManager.BSManagerServiceKey.Listing(listings)) =>
            listings.foreach(bm => context.log.info("BaseManager Path received from Receptionist Listing: {}", bm.path))
            Behaviors.same
        }
    }
  }
}