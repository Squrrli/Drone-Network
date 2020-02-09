package ie.ul.dronenet.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.{Actor, ActorLogging, Props}

object DroneManager {
  def apply(managerId: String, localDrone: ActorRef[Drone.Command]): Behavior[Receptionist.Listing] = {
    Behaviors
      .setup[Receptionist.Listing] { context =>
        context.log.debug("Drone Manager starting")

        context.system.receptionist ! Receptionist.Subscribe(Drone.DroneServiceKey, context.self)
//        context.system.receptionist ! Receptionist.Subscribe(BaseManager.BaseManagerServiceKey, context.self)

        Behaviors.receiveMessagePartial[Receptionist.Listing] {
          case Drone.DroneServiceKey.Listing(listings) =>
            listings.foreach(d => context.log.info("Drone Path received from Receptionist Listing: {}", d.path))
            Behaviors.same
//          case BaseManager.BaseManagerServiceKey.Listing(listings) =>
//            listings.foreach(bm => context.log.info("BaseManager Path received from Receptionist Listing: {}", bm.path))
//            Behaviors.same
        }
      }
  }
}