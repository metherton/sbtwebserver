package martinetherton.web

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.varwise.akka.http.prometheus.PrometheusResponseTimeRecorder
import com.varwise.akka.http.prometheus.api.{MetricFamilySamplesEntity, MetricsEndpoint}
import com.varwise.akka.http.prometheus.directives.ResponseTimeRecordingDirectives
import io.prometheus.client.{CollectorRegistry, Counter}
import martinetherton.actors.TreeImporter
import martinetherton.client.Request
import martinetherton.domain.Constants._
import martinetherton.domain._
import martinetherton.mappers.Marshallers
import martinetherton.persistence.{BranchRepository, LoserRepository, PersonRepository, ProfileRepository}
import spray.json._

import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.{Failure, Success}

object WebServerSave extends App with Marshallers  {

  implicit val system = ActorSystem("martinetherton-webserver")
  implicit val executionContext = system.dispatcher
  val branchRepo = new BranchRepository
  val personRepo = new PersonRepository
  val treeReader = system.actorOf(Props[TreeImporter], "TreeImporter")
  private val prometheusRegistry: CollectorRegistry = PrometheusResponseTimeRecorder.DefaultRegistry
  private val prometheusResponseTimeRecorder: PrometheusResponseTimeRecorder = PrometheusResponseTimeRecorder.Default
  private val metricsEndpoint = new MetricsEndpoint(prometheusRegistry)
  private val responseTimeDirectives = ResponseTimeRecordingDirectives(prometheusResponseTimeRecorder)
  private val r = scala.util.Random
  val webServerCounter = Counter.build().name("app_requests_total").help("Requests").register()

  val routing: Route = cors() {
    Route.seal {
      path("api" / "persons" ) {
        (extractRequest & extractLog) { (request, log) =>
          //print(s"$request.entity")
          val entity = request.entity
          val strictEntityFuture = entity.toStrict(2 seconds)
          val personParamsFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[PersonParams])
          onComplete(personParamsFuture) {
            case Success(person) =>
              log.info(s"Got person: $person")
              val result = personRepo.getPersons(person.firstName.trim, person.surname.trim)
              complete(result)
            case Failure(ex) =>
              failWith(ex)
          }
        }
      } ~
      path("api" / "branches" ) {
        val result = branchRepo.getBranches()
        complete(result)
      } ~
      path("hello" ) {
        import responseTimeDirectives._
        recordResponseTime("/hello") {
          complete {
            val sleepTime = r.nextLong(5000)
            Thread.sleep(sleepTime)
            HttpEntity(
              ContentTypes.`text/html(UTF-8)`,
              """
                |<html>
                |<body>
                |hello from the high level Akka HTTP
                |</body>
                |</html>
              """.stripMargin
            )
          }
        }
      } ~
      path("metrics" ) {
        complete {
          MetricFamilySamplesEntity.fromRegistry(prometheusRegistry)
        }
      } ~
      pathEndOrSingleSlash {
        complete(StatusCodes.OK)
      }
    }
  }

  val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bind(routing)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}