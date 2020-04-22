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
      val actorConfig = context.system.settings.config

      val singletonManager = ClusterSingleton(context.system)
      // Start if needed and provide a proxy to a named singleton
      val proxy: ActorRef[IOSocket.Command] = singletonManager.init(
        SingletonActor(Behaviors.supervise(IOSocket()).onFailure[Exception](SupervisorStrategy.restart), "IOSocket"))
      DroneNetworkApp.ioSocket = Some(proxy)

      // Create an actor that handles cluster domain events
      context.spawn(ClusterListener(), "ClusterListener")

      // Spawn desired actors depending on the current Node's role
      val selfMember = Cluster(context.system).selfMember
      if (selfMember.hasRole("Drone")) {
        val name = actorConfig.getAnyRef("drone.name").toString
        val dType = actorConfig.getAnyRef("drone.type").toString
        val range = actorConfig.getDouble("drone.range")
        val maxWeight = actorConfig.getDouble("drone.max-weight")

        val droneManager = context.spawn(DroneManager(name, dType, range, maxWeight), name+"-manager")

      } else if (selfMember.hasRole("Base")) {
        val name = actorConfig.getAnyRef("base-station.name").toString
        val cap = actorConfig.getDouble("base-station.max-capacity")
        val latlng = Seq(
          actorConfig.getDouble("base-station.loc.lat"),
          actorConfig.getDouble("base-station.loc.lng")
        )
        val baseManager = context.spawn(BaseManager(name, cap, latlng), name+"-manager")
        baseManager ! BaseManager.SetIOSocket(proxy)
      }
      Behaviors.same
    }
  }

  var ioSocket: Option[ActorRef[IOSocket.Command]] = None

  def main(args: Array[String]): Unit = {
    val ports =
      if (args.isEmpty)
        Seq(25251)
      else
        args.toSeq.map(_.toInt)
    ports.foreach(startup)
  }

  def chooseDrone(model: String, dataFile: String): String = Seq("minizinc", model, dataFile).!!

  def startup(port: Int): Unit = {
    // Override the configuration of the port
    val config = ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.port=$port
      """).withFallback(ConfigFactory.load())

    val system: ActorSystem[Any] = ActorSystem(RootBehavior(), "ClusterSystem", config)
  }
}
