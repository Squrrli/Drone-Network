package ie.ul.dronenet.actors

import akka.actor.typed
import akka.{NotUsed, actor}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, GroupRouter, Routers}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.Success
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.util.{ByteString, Timeout}

import scala.concurrent.{Await, ExecutionContextExecutor, Future, TimeoutException}
import scala.concurrent.duration._
import scala.collection.mutable
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ie.ul.dronenet.actors.BaseStation.MissionResponse
import spray.json._

import scala.util
import scala.util.Failure

//case class LatLng(lat: Double, lng: Double)
case class Mission(origin: List[Double], dest: List[Double], weight: Double, distance: Double)
object MissionJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
//  implicit val LatLngFormat = jsonFormat2(LatLng)
  implicit val MissionFormat = jsonFormat4(Mission)
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
  import MissionJsonSupport._

  val routerGroup: GroupRouter[BaseStation.Command] = Routers.group(BaseStation.BaseStationServiceKey)
  val router: ActorRef[BaseStation.Command] = context.spawn(routerGroup.withRoundRobinRouting(), "BaseStation-group")

  implicit val system: actor.ActorSystem = context.system.toClassic
  implicit val ec: ExecutionContextExecutor = system.dispatcher
//  implicit val timeout: Timeout = 3.second
  implicit val scheduler: Scheduler = context.system.scheduler
  var baseManager: ActorRef[BaseManager.Command] = _
  var baseStations: List[(String, Double, Double)] = _
  private var missionReqId: Int = 0 // TODO: maintain state after actor down/ restart

  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case _: TimeoutException => complete(s"${StatusCodes.RequestTimeout}")
    case _ => complete(s"${StatusCodes.InternalServerError}")
  }

  val route: Route = cors() (
    handleExceptions(exceptionHandler) {
      concat (
        get {
          path("get-stations") {
//            context.log.debug("/get-stations endpoint hit")
            val optStations: Future[Option[List[(String, Double, Double)]]] = getStations

            onSuccess(optStations) {
              case Some(stationsList) =>
//                context.log.debug(stationsList.toString())
                complete(stationsList.toJson.toString())
              case None               => complete(StatusCodes.InternalServerError)
            }
          }
        },
        post {
          entity(as[Mission]) { mission =>
            context.log.debug(s"Mission: [${mission.origin.head}, ${mission.origin(1)}] to [${mission.dest.head}, ${mission.dest(1)}] with ${mission.weight}g payload")

            implicit val timeout: Timeout = 5.second
            val res: Future[BaseStation.MissionResponse] = router.ask(ref => BaseStation.ExecuteMission(ref,
              (mission.origin.head, mission.origin(1)), (mission.dest.head, mission.dest(1)), mission.weight, mission.distance, missionReqId))
            missionReqId += 1

            onComplete(res) {
              case util.Success(value) =>
                context.log.debug(s"Successfully found Drone: ${value}")
                complete(value.toString)
              case Failure(exception) =>
                context.log.debug(s"FutureException: ${exception}")
                complete(StatusCodes.InternalServerError)
            }
          }
        }
      )
    }
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
    implicit val timeout: Timeout = 3.second
    val f: Future[BaseManager.AllDetailsResponse] = baseManager.ask(ref => BaseManager.GetAllStations(ref))
    f.map(res => Some(res.stations))
  }
}
