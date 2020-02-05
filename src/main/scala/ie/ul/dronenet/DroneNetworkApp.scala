package ie.ul.dronenet

import akka.actor.AddressFromURIString
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.typesafe.config.ConfigFactory
import ie.ul.dronenet.actors.{ClusterListener, NetworkSupervisor}
import com.typesafe.config.Config

import scala.io.StdIn

object DroneNetworkApp {

  object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
      // Create an actor that handles cluster domain events
      context.spawn(ClusterListener(), "ClusterListener")

      Behaviors.empty
    }
  }

  def main(args: Array[String]): Unit = {
    val ports =
      if (args.isEmpty)
        Seq(25251, 25252, 0)
      else
        args.toSeq.map(_.toInt)
    ports.foreach(startup)
  }

    def startup(port: Int): Unit = {
    // Override the configuration of the port
    val config = ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.port=$port
      """).withFallback(ConfigFactory.load())

    // Create an Akka system
    val system = ActorSystem[Nothing](RootBehavior(), "ClusterSystem", config)
  }
}
