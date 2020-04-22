package ie.ul.dronenet.actors

import akka.actor.typed
import akka.{NotUsed, actor}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import akka.util.{ByteString, Timeout}

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.collection.mutable
import spray.json._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import spray.json.DefaultJsonProtocol._
import spray.json._

case class Mission(lat: Double, lng: Double, weight: Double)
object PersonJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val MissionFormat = jsonFormat3(Mission)
}

object IOSocket {

  sealed trait Command extends CborSerializable

  final case class BaseStationResponse(stations: List[(String, Double, Double)]) extends Command
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
  import PersonJsonSupport._

  implicit val system: actor.ActorSystem = context.system.toClassic
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = 3.second
  implicit val scheduler: Scheduler = context.system.scheduler
  var baseManager: ActorRef[BaseManager.Command] = _
  var baseStations: List[(String, Double, Double)] = _

  val route: Route = cors() (
    concat (
      get {
        path("get-stations") {
          val optStations: Future[Option[List[(String, Double, Double)]]] = getStations

          onSuccess(optStations) {
            case Some(stationsList) =>
              context.log.debug(stationsList.toString())
              complete(stationsList.toJson.toString())
            case None               => complete(StatusCodes.InternalServerError)
          }
        }
      },
      post {
        entity(as[Mission]) { mission =>
          context.log.debug("Person: ${person.name} - favorite number: ${person.favoriteNumber}")
          complete(s"Mission: ${mission.lat}, ${mission.lng} with ${mission.weight}g payload")
        }
      }
    )
  )

  val bindingFuture: Future[Http.ServerBinding] = Http(system).bindAndHandle(route, "0.0.0.0", 8888)

  override def onMessage(msg: Command): Behavior[Command] = {
     msg match {
       case BaseStationResponse(stations) => baseStations = stations
       case SetBaseManagerRef(ref) => baseManager = ref
     }
    Behaviors.same
  }

  def getStations: Future[Option[List[(String, Double, Double)]]] = {
      val f: Future[BaseManager.AllDetailsResponse] = baseManager.ask(ref => BaseManager.GetAllStations(ref))
      f.map(res => Some(res.stations))
  }
}
