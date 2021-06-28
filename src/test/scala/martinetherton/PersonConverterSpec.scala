package martinetherton

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Flow, Framing, Keep, Sink, Source}
import akka.util.ByteString
import martinetherton.actors.TreeImporter
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

  it should "test sink" in {
    val sinkUnderTest = Flow[Int].map(_ * 2).toMat(Sink.fold(0)(_ + _))(Keep.right)

    val future = Source(1 to 4).runWith(sinkUnderTest)
    val result = Await.result(future, 3.seconds)
    assert(result == 20)
  }

  it should "test source" in {

    val sourceUnderTest = Source.repeat(1).map(_ * 2)

    val future = sourceUnderTest.take(10).runWith(Sink.seq)
    val result = Await.result(future, 3.seconds)
    assert(result == Seq.fill(10)(2))
  }

  it should "test flow" in {
    val flowUnderTest = Flow[Int].takeWhile(_ < 5)

    val future = Source(1 to 10).via(flowUnderTest).runWith(Sink.fold(Seq.empty[Int])(_ :+ _))
    val result = Await.result(future, 3.seconds)
    assert(result == (1 to 4))
  }

  it should "test factorial" in {
    val source: Source[Int, NotUsed] = Source(1 to 100)
    val factorials: Source[BigInt, NotUsed] = source.scan(BigInt(1))((acc, next) => acc * next)

    val result: Future[IOResult] =
      factorials.map(num => ByteString(s"$num\n")).runWith(FileIO.toPath(Paths.get("factorials.txt")))
  }

  it should "read file into Source[String...]" in {
    val stringSource: Source[String, Future[IOResult]] = new TreeImporter().stringArrayFrom("etherton-london-1.ged")
    val future = stringSource.runWith(Sink.head)
    val result = Await.result(future, 5.seconds)
    assert(result.startsWith("0 HEAD") == true)
  }

//  it should "output read file into String" in {
//    val stringSource: Source[String, Future[IOResult]] = WebServer.stringArrayFrom("etherton-london-1.ged")
// //   val future = stringSource.runForeach(i => println(i))
//  }

//  it should "filter only the lines we need" in {
//    val stringSource: Source[String, Future[IOResult]] = WebServer.stringArrayFrom("etherton-london-1.ged")
//
//    val filteredArray: Source[String, Future[IOResult]] = WebServer.getRequiredLines(stringSource)
//    val future: Future[Seq[String]] = filteredArray.runWith(Sink.seq)
//    val result = Await.result(future, 5.seconds)
//    assert(result.head.startsWith("0 @P7@ INDI") == true)
//  }

//  it should "group the filtered lines into person lines" in {
//    val stringSource: Source[String, Future[IOResult]] = WebServer.stringArrayFrom("etherton-london-1.ged")
//
//    val filteredArray: Source[String, Future[IOResult]] = WebServer.getRequiredLines(stringSource)
//    val personLines: Source[List[List[String]], Future[IOResult]] = WebServer.listOfPersonStringsFrom(filteredArray)
//    //val future = personLines.runForeach(l => println(l))
//    val future = personLines.runWith(Sink.head)
// //   val result = Await.result(future, 5.seconds)
////    result.foreach(l => l.foreach(x => println(x)))
//    //val future: Future[Seq[List[List[String]]]] = personLines.runWith(Sink.seq)
//
//  }

//  it should "get list of persons from arrays of person strings" in {
//    val stringSource: Source[String, Future[IOResult]] = WebServer.stringArrayFrom("etherton-london-1.ged")
//
//    val filteredArray: Source[String, Future[IOResult]] = WebServer.getRequiredLines(stringSource)
//    val personLines: Source[List[List[String]], Future[IOResult]] = WebServer.listOfPersonStringsFrom(filteredArray)
//    //val future = personLines.runForeach(l => println(l))
//    val persons = WebServer.personsFrom(personLines)
//    val future = persons.runWith(Sink.head)
//    val result = Await.result(future, 5.seconds)
//    assert(result.head.firstName === Some("Marie L "))
//
//  }
//
//  it should "filters persons with first name Percy" in {
//    val stringSource: Source[String, Future[IOResult]] = WebServer.stringArrayFrom("etherton-london-1.ged")
//
//    val filteredArray: Source[String, Future[IOResult]] = WebServer.getRequiredLines(stringSource)
//    val personLines: Source[List[List[String]], Future[IOResult]] = WebServer.listOfPersonStringsFrom(filteredArray)
//    //val future = personLines.runForeach(l => println(l))
//    val persons = WebServer.personsFrom(personLines)
//    val firstName = "Percy "
//    val personsCalledPercy = WebServer.filteredPersonList(persons, firstName, "*")
//    val future = personsCalledPercy.runWith(Sink.head)
//    val result = Await.result(future, 5.seconds)
//    assert(result.head.firstName === Some("Percy "))
//
//  }


}
