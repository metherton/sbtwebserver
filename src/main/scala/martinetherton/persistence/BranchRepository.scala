package martinetherton.persistence

import martinetherton.domain.{Branch, Person}

import scala.concurrent.Future

class BranchRepository {

  import slick.jdbc.MySQLProfile.api._

  class BranchTable(tag: Tag) extends Table[Branch](tag, "branch") {
    //def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def treeName = column[String]("treeName", O.PrimaryKey)
    def description = column[String]("description")
    def * = (treeName, description).mapTo[Branch]
  }

  val branches = TableQuery[BranchTable]

  val db = Database.forConfig("ons")

  def exec[T](action: DBIO[T]): Future[T] =
    db.run(action)

  def getBranches() =
    db.run(branches.result)

}
