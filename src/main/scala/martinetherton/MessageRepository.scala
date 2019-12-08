package martinetherton

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import slick.jdbc.H2Profile.api._
import martinetherton.Message

class MessageRepository {



  class MessageTable(tag: Tag) extends Table[Message](tag, "message") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def sender = column[String]("sender")
    def content = column[String]("content")
    def * = (sender, content, id.?).mapTo[Message]
  }

  val messages = TableQuery[MessageTable]
  val halSays = messages.filter(_.sender === "HAL")

  val db = Database.forConfig("chapter01")

  messages.schema.createStatements.mkString
  val action: DBIO[Unit] = messages.schema.create

  import scala.concurrent.Future
  val future: Future[Unit] = db.run(action)
  import scala.concurrent.Await
  import scala.concurrent.duration._
  val result = Await.result(future, 2.seconds)

  def freshTestData = Seq(
    Message("Dave", "Hello, HAL. Do you read me, HAL?", None),
    Message("HAL", "Affirmative, Dave. I read you.", None),
    Message("Dave", "Open the pod bay doors, HAL.", None),
    Message("HAL", "I'm sorry, Dave. I'm afraid I can't do that.", None)
  )

  val insert: DBIO[Option[Int]] = messages ++= freshTestData
  val insertAction: Future[Option[Int]] = db.run(insert)

  val rowCount = Await.result(insertAction, 2.seconds)



  def exec[T](action: DBIO[T]): Future[T] =
    db.run(action)

  def getMessages() = {
    val messagesAction: DBIO[Seq[Message]] = messages.result
    val messagesFuture: Future[Seq[Message]] = db.run(messagesAction)
    messagesFuture
  }

  def insert(message: Message) = {


    val ins = messages += message
    val insAct = exec(ins)
    insAct
  }

}
