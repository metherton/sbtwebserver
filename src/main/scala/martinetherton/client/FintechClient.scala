package martinetherton.client

import java.sql.Timestamp

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.server.Directives.onComplete
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import martinetherton.client.FintechClient.FindProfile
import martinetherton.domain.Constants.Host
import martinetherton.domain.{Loser, LoserDB, Profile, ProfileDB, Stock, StockDB, Url}
import martinetherton.mappers.Marshallers
import martinetherton.persistence.{LoserRepository, ProfileRepository, StockRepository}

import scala.concurrent.duration._
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object FintechClient {
  case object FindAllLosers
  case object FindAllStocks
  case object FindProfile
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
  implicit val profileToProfileDB = (p: Profile) =>
    ProfileDB(None, p.symbol, p.price, p.beta, p.volAvg, p.mktCap, p.lastDiv, p.range, p.changes, p.companyName, p.currency,
            p.cik, p.isin, p.cusip, p.exchange, p.exchangeShortName, p.industry, p.website, p.description, p.ceo, p.sector,
            p.country, p.fullTimeEmployees, p.phone, p.address, p.city, p.state, p.zip, p.dcfDiff, p.dcf, p.image, p.ipoDate, p.defaultImage, p.isEtf,
            p.isActivelyTrading, new Timestamp(System.currentTimeMillis()))


  val repoLoser = new LoserRepository
  val repoStock = new StockRepository
  val repoProfile = new ProfileRepository

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
    case FindProfile => {
      log.info("searching for all profile")
      val symbols = repoStock.getAllStocks()
//      val symbols = Future {List("XOP")}
      symbols.onComplete {
        case Success(stocks) => {
          val extractProfile = Flow[Stock].map(stock => stock.symbol)
          val simpleFlow = Flow[String].map(company => Request(Host("fintech"), Url(List("profile", company), Nil)).get
            .onComplete {
              case Success(response) => {
                val strictEntityFuture = response.entity.toStrict(10 seconds)
                val profilesFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[List[Profile]])
                profilesFuture.onComplete {
                  case Success(profiles) => {
                    val insAct = repoProfile.insert(profiles.map(profileToProfileDB))
                    insAct.onComplete {
                      case Success(result) => println(s"new person added with id: ${result}")
                      case Failure(ex) => println(s"could not insert: $ex")
                    }
                  }
                  case Failure(ex) => println(s"could not insert - failed: $ex") //throw ex
                }
              }
              case Failure(ex) => println(s"I have failed with $ex")
            })
          val simpleSink = Sink.foreach[Unit](println)
          import scala.concurrent.duration._
          val bufferedFlow = simpleFlow.buffer(1, overflowStrategy = OverflowStrategy.dropHead)
          val graph = Source(stocks).throttle(2, 1 second).async
            .map(stock => stock.symbol)
            .viaMat(bufferedFlow)(Keep.right).async
            .toMat(simpleSink)(Keep.right)
          graph.run().onComplete {
            case Success(_) => "Stream processing finished"
            case Failure(ex) => println(s"Stream processing finished with: $ex")
          }
        }
        case Failure(ex) => println(ex)
      }

    }
  }
}

