package martinetherton

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.util.{Random, Success}
import scala.io.StdIn
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.Done
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes

import scala.collection.mutable.ArrayBuffer
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.io.StdIn

import scala.concurrent.Future

import akka.actor.{ Actor, ActorSystem, Props, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn

import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import slick.basic.DatabasePublisher
import slick.jdbc.H2Profile.api._

//#imports
// Use H2Profile to connect to an H2 database
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global
//#imports
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import scala.collection.mutable.ArrayBuffer

import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global

object WebServer {

  val lines = new ArrayBuffer[Any]()
  def println(s: Any) = lines += s


  // needed to run the route
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext = system.dispatcher

  var orders: List[Item] = Nil

  // domain model
  final case class Item(id: Long, name: String)
  final case class Order(items: List[Item])
  final case class ItemVo(id: Long, name: String)


//  def name = column[String]("COF_NAME", O.PrimaryKey)
//  def supID = column[Int]("SUP_ID")
//  def price = column[Double]("PRICE")
//  def sales = column[Int]("SALES")
//  def total = column[Int]("TOTAL")
//  def * = (name, supID, price, sales, total)
//  // A reified foreign key relation that can be navigated to create a join
//  def supplier = foreignKey("SUP_FK", supID, suppliers)(_.id)

  // formats for unmarshalling and marshalling
  implicit val itemFormat = jsonFormat2(Item)
  implicit val orderFormat = jsonFormat1(Order)
  implicit val itemVoFormat = jsonFormat2(ItemVo)

  // (fake) async database query api
  def fetchItem(itemId: Long): Future[Option[Item]] = Future {
    orders.find(o => o.id == itemId)
  }
  def saveOrder(order: Order): Future[Done] = {
    orders = order match {
      case Order(items) => items ::: orders
      case _            => orders
    }
    Future { Done }
  }

 // implicit def listVoToOrder(vos: Seq[ItemVo]) = vos.map(v => Item(v.id, v.name))

  def main(args: Array[String]) {




    // Definition of the COFFEES table
    class Items(tag: Tag) extends Table[ItemVo](tag, "ITEMS") {
      def id = column[Long]("ID", O.PrimaryKey)
      def name = column[String]("ITEM_NAME")
      def * = (id, name) <> (ItemVo.tupled, ItemVo.unapply)
      // A reified foreign key relation that can be navigated to create a join
    }
    val table = TableQuery[Items]
    //#tables

    // Connect to the database and execute the following block within a session
    //#setup
    val db = Database.forConfig("h2mem1")
    try {
      // val resultFuture: Future[_] = { ... }
      //#setup

      //#create
      val setup = DBIO.seq(
        // Create the tables, including primary and foreign keys
        (table.schema).create,

        // Insert some coffees (using JDBC's batch insert feature, if supported by the DB)
        table ++= Seq(
          ItemVo(1, "Colombian"),
          ItemVo(2, "French_Roast")
        )
        // Equivalent SQL code:
        // insert into COFFEES(COF_NAME, SUP_ID, PRICE, SALES, TOTAL) values (?,?,?,?,?)
      )

      val setupFuture = db.run(setup)
      //#create
      val resultFuture = setupFuture.flatMap { _ =>

        //#readall
        // Read all coffees and print them to the console
        println("Items:")
        db.run(table.result).map(_.foreach {
          case ItemVo(id, name) =>
            println("  " + id + "\t" + name)
        })
        // Equivalent SQL code:
        // select COF_NAME, SUP_ID, PRICE, SALES, TOTAL from COFFEES
        //#readall

      }.flatMap { _ =>

        //#projection
        // Why not let the database do the string conversion and concatenation?
        //#projection
        println("Items (concatenated by DB):")
        //#projection
        val q1 = for(c <- table)
          yield LiteralColumn("  ") ++ c.name
        // The first string constant needs to be lifted manually to a LiteralColumn
        // so that the proper ++ operator is found

        // Equivalent SQL code:
        // select '  ' || COF_NAME || '\t' || SUP_ID || '\t' || PRICE || '\t' SALES || '\t' TOTAL from COFFEES

        db.stream(q1.result).foreach(println)
        //#projection

      }
      //#setup
      Await.result(resultFuture, Duration.Inf)
      lines.foreach(Predef.println _)
    }
    //#setup

    def findAllItems(): Future[Seq[ItemVo]] = {
      db.run(table.result)
    }

//    def findAllItems(): Future[Seq[ItemVo]] = Future {
//      Seq(ItemVo(1, "bla"), ItemVo(2, "uuu"))
//    }

    val route: Route =
      concat(
        get {
            path("items") {

            println("Coffees:")
      //      val blas = db.run(table.result)
            //val mapblas = blas.map(x => CoffeeVo(x.name, x.supId))
//            val blas = db.run(coffees.result).map(_.foreach {
//              case (name, supID, price, sales, total) =>
//                println("  " + name + "\t" + supID + "\t" + price + "\t" + sales + "\t" + total)
//            })
            println("blas")
      //      println(blas)
            println("thereitis")
        //    complete(orders)

            val result = findAllItems()
            complete(result)

          }
        },
        get {
          pathPrefix("item" / LongNumber) { id =>
            // there might be no item for a given id
            val maybeItem: Future[Option[Item]] = fetchItem(id)

            onSuccess(maybeItem) {
              case Some(item) => complete(item)
              case None       => complete(StatusCodes.NotFound)
            }
          }
        },
        post {
          path("create-order") {
            entity(as[Order]) { order =>
              val saved: Future[Done] = saveOrder(order)
              onComplete(saved) { done =>
                complete("order created")
              }
            }
          }
        }
      )

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }



}