package ie.ul.dronenet

import akka.actor.AddressFromURIString
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.cluster.typed.Cluster
import com.typesafe.config.ConfigFactory
import ie.ul.dronenet.actors.{ClusterListener, Drone, NetworkSupervisor}
import com.typesafe.config.Config

import scala.io.StdIn

object DroneNetworkApp {

  object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
      // Create an actor that handles cluster domain events
      context.spawn(ClusterListener(), "ClusterListener")

      // TODO: move all root behavior to Network supervisor
      val selfMember = Cluster(context.system).selfMember
      if(selfMember.hasRole("Drone")) {
        context.spawn(Drone("testDrone1"), "Drone")
      }

      Behaviors.empty
    }
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
