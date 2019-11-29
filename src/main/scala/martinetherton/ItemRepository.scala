package martinetherton
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"

//#imports
// Use H2Profile to connect to an H2 database
//#imports
import slick.jdbc.H2Profile.api._

import scala.concurrent.Future

class ItemRepository(db: slick.jdbc.H2Profile.backend.DatabaseDef) {

  class Items(tag: Tag) extends Table[ItemVo](tag, "ITEMS") {
    def id = column[String]("ID")
    def name = column[String]("ITEM_NAME")
    def * = (id, name) <> (ItemVo.tupled, ItemVo.unapply)
    // A reified foreign key relation that can be navigated to create a join
  }

  val table = TableQuery[Items]

  def findAllItems(): Future[Seq[ItemVo]] = {
    db.run(table.result)
  }
}
