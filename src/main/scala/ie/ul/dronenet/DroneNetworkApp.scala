package ie.ul.dronenet

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.cluster.typed.{Cluster, ClusterSingleton, SingletonActor}
import com.typesafe.config.ConfigFactory
import ie.ul.dronenet.actors.{BaseManager, BaseStation, ClusterListener, Drone, DroneManager, IOSocket, NetworkSupervisor}
import com.typesafe.config.Config

import scala.io.StdIn
import scala.sys.process._

object DroneNetworkApp {

  /**
   * Root actor behavior, guardian to each Cluster node
   * Spawns cluster listener and the correct actors based on a Node's role
   */
  object RootBehavior {
    def apply(): Behavior[Any] = Behaviors.setup[Any] { context =>


      val singletonManager = ClusterSingleton(context.system)
      // Start if needed and provide a proxy to a named singleton
      val proxy: ActorRef[IOSocket.Command] = singletonManager.init(
        SingletonActor(Behaviors.supervise(IOSocket()).onFailure[Exception](SupervisorStrategy.restart), "IOSocket"))
      DroneNetworkApp.ioSocket = Some(proxy)

      // Create an actor that handles cluster domain events
      context.spawn(ClusterListener(), "ClusterListener")
//      context.spawn(IOSocket(), "IOSocket")

      // Spawn desired actors depending on the current Node's role
      val selfMember = Cluster(context.system).selfMember
      if (selfMember.hasRole("Drone")) {
        val droneManager = context.spawn(DroneManager("testDroneManager1"), "DroneManager")
//        context.spawn(Drone("testDroneManager1", droneManager), "Drone")
      } else if (selfMember.hasRole("Base")) {
        val baseManager = context.spawn(BaseManager("testBaseManager1"), "BSManager")
        baseManager ! BaseManager.SetIOSocket(proxy)
      }
      Behaviors.same
    }
  }

  var ioSocket: Option[ActorRef[IOSocket.Command]] = None

  // TODO: remove multiple systems on single JVM in future release
  def main(args: Array[String]): Unit = {
    val ports =
      if (args.isEmpty)
        Seq(25251, 25252, 0)
      else
        args.toSeq.map(_.toInt)
    ports.foreach(startup)
  }

  def chooseDrone(model: String, df: String): String = Seq("minizinc", model, df).!!

  // Start new cluster node for each of the given ports
  def startup(port: Int): Unit = {
    // Override the configuration of the port
    val config = ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.port=$port
      """).withFallback(ConfigFactory.load())

    // Create an Akka system
    val system: ActorSystem[Any] = ActorSystem(RootBehavior(), "ClusterSystem", config)
  }
}
