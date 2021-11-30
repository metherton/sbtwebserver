package martinetherton.web

import java.io.{File, InputStream}
import java.security.{KeyStore, SecureRandom}

import akka.Done
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.headers.{HttpCookie, SameSite}
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.http.scaladsl.model.{ContentTypes, DateTime, HttpEntity, Multipart, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{FileIO, Framing, Sink, Source}
import akka.util.ByteString
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

import scala.concurrent.Future
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

  case class P(firstName: String, surname: String)
  class Pers {
    var firstName = ""
    var surname = ""
  }

  var myPers = new Pers

  val routing: Route = cors() {
    Route.seal {
      path("api" / "upload" ) {
        (extractLog) { (log) =>
          // handle uploading files
          // multipart / form data
          entity(as[Multipart.FormData]) { formdata =>
            // handle file payload
            val partsSource: Source[Multipart.FormData.BodyPart, Any] = formdata.parts
            val filePartsSink: Sink[Multipart.FormData.BodyPart, Future[Done]] = Sink.foreach[Multipart.FormData.BodyPart] { bodyPart =>
              if (bodyPart.name == "myFile") {
                // create a file
                val filename = bodyPart.filename.getOrElse("tempFile_" + System.currentTimeMillis())
                val file = new File(filename)


                log.info(s"writing to file $filename")

                val fileContentsSource: Source[ByteString, _] = bodyPart.entity.dataBytes
                val fileContentSink: Sink[ByteString, _] = FileIO.toPath(file.toPath)

                fileContentsSource.runWith(fileContentSink)
              }
              else if (bodyPart.name == "firstName") {
                myPers.firstName = bodyPart.entity.asInstanceOf[HttpEntity.Strict].data.utf8String
              }
              else if (bodyPart.name == "surname") {
                myPers.surname = bodyPart.entity.asInstanceOf[HttpEntity.Strict].data.utf8String
              }
            }

            val writeOperationFuture = partsSource.runWith(filePartsSink)
            onComplete(writeOperationFuture) {
              case Success(_) => {
                var p = new P("bob", "smith")

                complete("File uploaded" + s"person uploaded ${myPers.firstName} ${myPers.surname}")
              }
              case Failure(ex) => complete(s"File failed to upload $ex")
            }
          }
        }
      } ~
      path("upload" ) {
        (extractLog) { (log) =>
          // handle uploading files
          // multipart / form data
          entity(as[Multipart.FormData]) { formdata =>
            // handle file payload
            val partsSource: Source[Multipart.FormData.BodyPart, Any] = formdata.parts
            var firstName1: String = ""
            var surname1: String = ""
            val filePartsSink: Sink[Multipart.FormData.BodyPart, Future[Done]] = Sink.foreach[Multipart.FormData.BodyPart] { bodyPart =>
              if (bodyPart.name == "myFile") {
                // create a file
//                val filename = "src/main/resources/download/" + bodyPart.filename.getOrElse("tempFile_" + System.currentTimeMillis())

                val filename = bodyPart.filename.getOrElse("tempFile_" + System.currentTimeMillis())
                val file = new File(filename)


                log.info(s"writing to file $filename")

                val fileContentsSource: Source[ByteString, _] = bodyPart.entity.dataBytes
                val fileContentSink: Sink[ByteString, _] = FileIO.toPath(file.toPath)

                fileContentsSource.runWith(fileContentSink)
              }
              else if (bodyPart.name == "firstName") {
                myPers.firstName = bodyPart.entity.asInstanceOf[HttpEntity.Strict].data.utf8String
              }
              else if (bodyPart.name == "surname") {
                myPers.surname = bodyPart.entity.asInstanceOf[HttpEntity.Strict].data.utf8String
              }
            }

            val writeOperationFuture = partsSource.runWith(filePartsSink)
            onComplete(writeOperationFuture) {
              case Success(_) => {
                var p = new P("oo", "you")
                complete("File uploaded" + s"person uploaded ${myPers.firstName} ${myPers.surname}")
              }
              case Failure(ex) => complete(s"File failed to upload $ex")
            }
          }
        }
      } ~
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
      path("persons" / Segment ) { tree => {
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