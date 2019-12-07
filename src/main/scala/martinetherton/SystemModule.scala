package martinetherton

import slick.jdbc.H2Profile.api._
//object SystemModule {
//
//}

object SystemModule {


  val messageRepository = new MessageRepository()
  def messageRepo = messageRepository

}
