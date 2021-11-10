package martinetherton.web

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.varwise.akka.http.prometheus.PrometheusResponseTimeRecorder
import com.varwise.akka.http.prometheus.api.{MetricFamilySamplesEntity, MetricsEndpoint}
import com.varwise.akka.http.prometheus.directives.ResponseTimeRecordingDirectives
import io.prometheus.client.{CollectorRegistry, Counter}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
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

object WebServer extends App with Marshallers  {

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
      path("persons" ) {
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
      path("api" / "persons" / Segment ) { tree => {
          (extractRequest & extractLog) { (request, log) =>
            //print(s"$request.entity")
            val entity = request.entity
            val strictEntityFuture = entity.toStrict(2 seconds)
            val personParamsFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[PersonParams])
            onComplete(personParamsFuture) {
              case Success(person) =>
                log.info(s"Got person: $person")
                val result = personRepo.getPersons(person.firstName.trim, person.surname.trim, tree.trim)
                complete(result)
              case Failure(ex) =>
                failWith(ex)
            }
          }
        }
      } ~
      path("branches" ) {
        val result = branchRepo.getBranches()
        complete(result)
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

//  val bindingFuture = Http().newServerAt("0.0.0.0", 8443).enableHttps(https).bind(routing)
  val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bind(routing)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done


//    def https() = {
//      val password: Array[Char] = "akka-https".toCharArray // do not store passwords in code, read them from somewhere safe!
//      val ks: KeyStore = KeyStore.getInstance("PKCS12")
//      val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("keystore.pkcs12")
//      require(keystore != null, "Keystore required!")
//      ks.load(keystore, password)
//      val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
//      keyManagerFactory.init(ks, password)
//      val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
//      tmf.init(ks)
//      val sslContext: SSLContext = SSLContext.getInstance("TLS")
//      sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
//      val https: HttpsConnectionContext = ConnectionContext.httpsServer(sslContext)
//
//      https
//    }

}