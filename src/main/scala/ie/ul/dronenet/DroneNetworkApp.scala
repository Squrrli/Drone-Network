package ie.ul.dronenet

import akka.actor.ActorSystem
import ie.ul.dronenet.actors.NetworkSupervisor

import scala.io.StdIn

object DroneNetworkApp {
  def main(args:  Array[String]): Unit = {
    val system = ActorSystem("Drone-Network")

    try {
      val networkSupervisor = system.actorOf(NetworkSupervisor.props(), "Drone-Network-Supervisor")
      // wait for user to exit program
      StdIn.readLine()
    } finally {
      system.terminate()
    }
  }
}
