package martinetherton

import slick.jdbc.H2Profile.backend.Database

//object SystemModule {
//
//}

object SystemModule {

  val db = Database.forConfig("h2mem1")
  val itemRepository = new ItemRepository(db)

  def itemRepo = itemRepository

}
