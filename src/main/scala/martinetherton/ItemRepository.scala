package martinetherton
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"

//#imports
// Use H2Profile to connect to an H2 database
//#imports
import akka.Done
import slick.jdbc.H2Profile.api._

import scala.concurrent.Future

class ItemRepository(db: slick.jdbc.H2Profile.backend.DatabaseDef) {

  class Items(tag: Tag) extends Table[ItemVo](tag, "ITEMS") {
    def id = column[Long]("ID", O.PrimaryKey)
    def name = column[String]("ITEM_NAME")
    def * = (id, name) <> (ItemVo.tupled, ItemVo.unapply)
    // A reified foreign key relation that can be navigated to create a join
  }

  val table = TableQuery[Items]

  def findAllItems(): Future[Seq[ItemVo]] = {
    db.run(table.result)
  }

//  def saveItem(item: ItemVo): Future[Done] = {
//    import scala.concurrent.ExecutionContext.Implicits.global
//    db.executor(table returning table.map(_.id) += item)
//    Future {Done}
//  }

  def saveItem(item: ItemVo) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      DBIO.seq(table += ItemVo(3, "blana"))
    }
  }

}
