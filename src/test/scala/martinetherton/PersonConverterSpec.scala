package martinetherton

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Framing, Sink, Source}
import akka.util.ByteString
import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class PersonConverterSpec extends UnitSpec {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()

  behavior of "Person Converter"

  it should "tokenize the ByteString on \r\n into an array of utf8Strings " in {
    val file = Paths.get(ClassLoader.getSystemResource("test-etherton-london-1.ged").toURI)
    val source = FileIO.fromPath(file)
    val bla: Source[ByteString, Future[IOResult]] = source.via(Framing.delimiter(
      ByteString("\r\n"), maximumFrameLength = 500, allowTruncation = true))
    val future: Future[Seq[ByteString]] = bla.runWith(Sink.seq)
    val result: Seq[ByteString] = Await.result(future, 5.seconds)
    assert(result.toList.length === 10)
    result.map(_.utf8String).toList.head shouldBe "0 HEAD"
  }


}
