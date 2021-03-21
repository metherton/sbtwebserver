package martinetherton.client

import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Sink, Source}
import javax.net.ssl.SSLContext
import martinetherton.domain.Constants.{SslPort}
import martinetherton.web.WebServer.system

import scala.concurrent.Future

object Request {
  def apply(host: String, resource: String): Request = new Request(host, resource)
}

class Request(host: String, resource: String) {

  implicit val executionContext = system.dispatcher
  implicit val httpsConnectionContext = ConnectionContext.https(SSLContext.getDefault)
  implicit val connectionFlow = Http().outgoingConnectionHttps(host, SslPort, httpsConnectionContext)

  def get(): Future[HttpResponse] = {
    Source.single(HttpRequest(uri = resource)).via(connectionFlow).runWith(Sink.head)
  }

}
