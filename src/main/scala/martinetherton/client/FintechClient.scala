package martinetherton.client

import java.sql.Timestamp

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.server.Directives.onComplete
import akka.stream.ActorMaterializer
import martinetherton.domain.Constants.Host
import martinetherton.domain.{Loser, LoserDB, Stock, StockDB, Url}
import martinetherton.mappers.Marshallers
import martinetherton.persistence.{LoserRepository, StockRepository}

import scala.concurrent.duration._
import spray.json._

import scala.util.{Failure, Success}

object FintechClient {
  case object FindAllLosers
  case object FindAllStocks
}

class FintechClient extends Actor with ActorLogging with Marshallers {

  import FintechClient._
  implicit val system = ActorSystem("Fintech")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher


  implicit val loserToLoserDB = (loser: Loser) =>
    LoserDB(None, loser.ticker, loser.changes, loser.price, loser.changesPercentage, loser.companyName, new Timestamp(System.currentTimeMillis()))
  implicit val stockToStockDB = (stock: Stock) =>
    StockDB(None, stock.symbol, stock.price, new Timestamp(System.currentTimeMillis()))


  val repoLoser = new LoserRepository
  val repoStock = new StockRepository

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
              val insAct = repoLoser.insert(losers.map(loserToLoserDB))
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
    case FindAllStocks => {
      log.info("searching for all stocks")
      val result = Request(Host("fintech"), Url(List("stock", "list"), Nil)).get
      result.onComplete {
        case Success(response) => {
          val strictEntityFuture = response.entity.toStrict(10 seconds)
          val stocksFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[List[Stock]])
          stocksFuture.onComplete {
            case Success(stocks) => {
              val insAct = repoStock.insert(stocks.map(stockToStockDB))
              insAct.onComplete {
                case Success(result) => println(s"new stocks added with id: ${result}")
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

