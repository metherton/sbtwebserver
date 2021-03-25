package martinetherton.client

import java.sql.Timestamp

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.stream.ActorMaterializer
import martinetherton.domain.Constants.Host
import martinetherton.domain.{Loser, LoserDB, Url}
import martinetherton.mappers.Marshallers
import martinetherton.persistence.LoserRepository
import scala.concurrent.duration._
import spray.json._

import scala.util.{Failure, Success}

object FintechClient {
  case object FindAllLosers
}

class FintechClient extends Actor with ActorLogging with Marshallers {

  import FintechClient._
  implicit val system = ActorSystem("Fintech")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher


  implicit val loserToLoserDB = (loser: Loser) =>
    LoserDB(None, loser.ticker, loser.changes, loser.price, loser.changesPercentage, loser.companyName, new Timestamp(System.currentTimeMillis()))

  val repo = new LoserRepository

  override def receive: Receive = {
    case FindAllLosers => {
      log.info("searching for all losers")
      val result = Request(Host("fintech"), Url(List("losers"), Nil)).get
      result.onComplete {
        case Success(response) => {
          val strictEntityFuture = response.entity.toStrict(10 seconds)
          val losersFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[List[Loser]])
          losersFuture.onComplete {
            case Success(losers) => {
              val insAct = repo.insert(losers.map(loserToLoserDB))
              insAct.onComplete {
                case Success(result) => println(s"new person added with id: ${result}")
                case Failure(ex) => println(s"could not insert: $ex")
              }
            }
            case Failure(ex) => println(s"Really, I have failed with $ex")
          }
        }
        case Failure(ex) => println(s"I have failed with $ex")
      }
    }
  }
}

