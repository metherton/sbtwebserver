package martinetherton

case class Message(sender: String, content: String, id: Option[Long] = None)
