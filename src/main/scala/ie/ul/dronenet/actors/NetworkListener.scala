package ie.ul.dronenet.actors

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.{Actor, ActorLogging, Props}

object NetworkListener {
  def props(): Props = Props(new NetworkListener)
}

/**
 * Drone Actor to listen to and log Cluster events
 */
class NetworkListener extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  // Subscribe to cluster events onStart/Restart
  override def preStart(): Unit = {
    log.warning("NetworkListener starting, subscribing to Cluster")
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
                      classOf[MemberEvent], classOf[UnreachableMember])
  }

  // Unsubscribe before stop to ensure events not being sent to dead listener
  override def postStop(): Unit = cluster.unsubscribe(self)


  override def receive: Receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info(
        "Member is Removed: {} after {}",
        member.address, previousStatus)
    case _: MemberEvent => // ignore
  }
}
