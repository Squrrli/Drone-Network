package ie.ul.dronenet.actors

import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, UpgradeToWebSocket}
import akka.stream.scaladsl.{Flow, Framing, Sink, Source, Tcp}
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.util.ByteString

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

sealed trait Command extends CborSerializable

object IOSocket {
  def apply(): Behavior[Command] = Behaviors.setup(
    context => new IOSocket(context)
  )
}

/**
 * Class to handle communication between frontend and Drone Network
 * Singleton actor through with all socket communication takes place.
 */
class IOSocket(context: ActorContext[Command]) extends AbstractBehavior[Command](context) {
  implicit val system: ActorSystem[Nothing] = context.system
  implicit val ec: ExecutionContextExecutor = system.executionContext

  val networkWebSocketService: Flow[Message, TextMessage, _] =
    Flow[Message]
      .mapConcat {
        // we match but don't actually consume the text message here,
        // rather we simply stream it back as the tail of the response
        // this means we might start sending the response even before the
        // end of the incoming message has been received
        case tm: TextMessage => TextMessage(Source.single("Hello ") ++ tm.textStream) :: Nil
        case bm: BinaryMessage =>
          // ignore binary messages but drain content to avoid the stream being clogged
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }

  val requestHandler: HttpRequest => HttpResponse = {
    case req @ HttpRequest(GET, Uri.Path("/network"), _, _, _) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upgrade) => upgrade.handleMessages(networkWebSocketService)
        case None          => HttpResponse(400, entity = "Not a valid websocket request!")
      }
    case r: HttpRequest =>
      r.discardEntityBytes() // important to drain incoming HTTP Entity stream
      HttpResponse(404, entity = "Unknown resource!")
  }

  val bindingFuture: Future[Http.ServerBinding] =
    Http(system).bindAndHandleSync(requestHandler, interface = "localhost", port = 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done


  //  val connections: Source[IncomingConnection, Future[ServerBinding]] =
  //    Tcp(system).bind("127.0.0.1", 8888)

//  connections.runForeach { connection =>
//    println(s"New connection from: ${connection.remoteAddress}")
//
//    val parser  = Flow[String].takeWhile(_ != "q").map(_ + "!")
//
//    import connection._
//    val welcomeMsg = s"Welcome $remoteAddress, you are now connected to $localAddress"
//    val welcome = Source.single(welcomeMsg)
//
//    val echo = Flow[ByteString]
//      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
//      .map(_.utf8String)
//      .via(parser)
//      .merge(welcome)
//      .map(ByteString(_))
//
//    connection.handleWith(echo)
//  }

  override def onMessage(msg: Command): Behavior[Command] = ???
}
