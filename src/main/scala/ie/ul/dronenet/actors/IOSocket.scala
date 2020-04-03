package ie.ul.dronenet.actors

import akka.{NotUsed, actor}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Graph, Materializer, SinkShape}
import akka.stream.scaladsl.{Flow, Framing, Sink, Source, Tcp}
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.util.{ByteString, Timeout}
import akka.pattern.ask

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.io.StdIn
import scala.collection.mutable
import scala.util.{Failure, Success}

object IOSocket {

  sealed trait Command extends CborSerializable

  final case class BaseStationResponse(stations: mutable.Set[ActorRef[BaseStation.Command]]) extends Command
  final case class SetBaseManagerRef(ref: ActorRef[BaseManager.Command]) extends Command

  case class AdaptedResponse(res: mutable.Set[ActorRef[BaseStation.Command]]) extends Command

  def apply(): Behavior[Command] = Behaviors.setup(
    context => new IOSocket(context)
  )
}

/**
 * Class to handle communication between frontend and Drone Network
 * Singleton actor through with all socket communication takes place.
 */
class IOSocket(context: ActorContext[IOSocket.Command]) extends AbstractBehavior[IOSocket.Command](context) {
  import IOSocket._

  implicit val system: actor.ActorSystem = context.system.toClassic
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = 3.second
  var baseManager: ActorRef[BaseManager.Command] = _
  var baseStations: String = _

  val route: Route = concat (
    get {
      path("get-stations") {
        val optStations: Future[Option[(String, Float, Float)]] = getStations

        onSuccess(optStations) {
          case Some(stationsList) => complete(stationsList.toString())
          case None               => complete(StatusCodes.InternalServerError)
        }
      }
    }
  )

  val bindingFuture: Future[Http.ServerBinding] = Http(system).bindAndHandle(route, "127.0.0.1", 8888)

  override def onMessage(msg: Command): Behavior[Command] = {
     msg match {
       case BaseStationResponse(stations) => baseStations = stations.toString()
       case SetBaseManagerRef(ref) => baseManager = ref
     }
    Behaviors.same
  }

  def getStations: Future[Option[(String, Float, Float)]] = {
    // TODO: get a list of stations from station managers -> managers should format information as needed
    Future.successful(Some(("http-test", 9000, 9001)))
  }
}
