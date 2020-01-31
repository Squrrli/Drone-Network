package ie.ul.dronenet.actors

import akka.actor.{Actor, ActorLogging, Props}
import ie.ul.dronenet.actors.Drone._

object NetworkSupervisor {
  def props(): Props = Props(new NetworkSupervisor)
}

class NetworkSupervisor extends Actor with ActorLogging {
  override def preStart(): Unit = {
    log.info("DroneNetwork Started")
//    setupTestDrone()
  }

  override def postStop(): Unit = log.info("Drone-Network exiting")

  def setupTestDrone(): Unit = {
    val drone1 = context.actorOf(Drone.props(0, 1))
    drone1 ! Ping
    drone1 ! AssignMission(2314, "TestMission1")
  }

  override def receive: Receive = Actor.emptyBehavior
}
