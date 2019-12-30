package ie.ul.dronenet.actors

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.{Actor, ActorLogging}

/**
 * Drone Actor to listen to and log Cluster events
 */
class NetworkListener extends Actor with ActorLogging{
  override def receive: Receive = ???
}
