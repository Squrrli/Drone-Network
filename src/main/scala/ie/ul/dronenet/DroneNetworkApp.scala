package ie.ul.dronenet

import akka.actor.AddressFromURIString
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.cluster.typed.Cluster
import com.typesafe.config.ConfigFactory
import ie.ul.dronenet.actors.{BaseManager, BaseStation, ClusterListener, Drone, DroneManager, NetworkSupervisor}
import com.typesafe.config.Config

import scala.io.StdIn

object DroneNetworkApp {

  /**
   * Root actor behavior, guardian to each Cluster node
   * Spawns cluster listener and the correct actors based on a Node's role
   */
  object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Receptionist.Listing] { context =>
      // Create an actor that handles cluster domain events
      context.spawn(ClusterListener(), "ClusterListener")

      // Subscribe to Cluster Receptionist. Used for ActorRef discovery across the cluster
//      context.system.receptionist ! Receptionist.Subscribe(Drone.DroneServiceKey, context.self) // TODO: move to respective actors, or forward relevant messages to those actors

      // Spawn desired actors depending on the current Node's role
      val selfMember = Cluster(context.system).selfMember
      if( selfMember.hasRole("Drone")) {
        val drone = context.spawn(Drone("testDrone1"), "Drone")
        context.spawn(DroneManager("testDroneManager1", drone), "DroneManager")
      } else if (selfMember.hasRole("Base")) {
        val base = context.spawn(BaseStation("testBase1"), "BaseStation")
        context.spawn(BaseManager("testBaseManager1", base), "BSManager")
      }


      Behaviors.receiveMessagePartial[Receptionist.Listing] {
        case Drone.DroneServiceKey.Listing(listings) =>
//          listings.foreach(d => context.log.debug("Receptionist listing for {}", d.path))
          Behaviors.same
      }
    } // Narrow Behavior[T] type T to match apply() return type of Behavior[Nothing]
     .narrow
  }

  // TODO: remove multiple systems on single JVM in future release
  def main(args: Array[String]): Unit = {
    val ports =
      if (args.isEmpty)
        Seq(25251, 25252, 0)
      else
        args.toSeq.map(_.toInt)
    ports.foreach(startup)
  }

  // Start new cluster node for each of the given ports
    def startup(port: Int): Unit = {
    // Override the configuration of the port
    val config = ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.port=$port
      """).withFallback(ConfigFactory.load())

    // Create an Akka system
    val system = ActorSystem[Nothing](RootBehavior(), "ClusterSystem", config)
  }
}
