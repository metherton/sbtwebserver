package martinetherton

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import martinetherton.actors.TreeImporter
import martinetherton.actors.TreeImporter.ImportTree
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration.Duration

class TreeImporterSpec extends WordSpecLike with BeforeAndAfterAll {
  implicit val system = ActorSystem("SynchronousTestingSpec")

  override def afterAll(): Unit = {
    system.terminate()
  }

  "A tree importer" should {
    "parse the file" in {
      val treeImporter = TestActorRef[TreeImporter](Props[TreeImporter])
      treeImporter ! ImportTree // counter has already received the message

    }

  }

}
//object TreeImporterSpec {
//  case object Inc
//  case object Read
//  class Counter extends Actor {
//    var count = 0
//    override def receive: Receive = {
//      case Inc => count += 1
//      case Read => sender() ! count
//    }
//  }
//}
