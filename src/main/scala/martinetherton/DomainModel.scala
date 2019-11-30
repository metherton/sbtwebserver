package martinetherton

final case class ItemVo(id: Long, name: String)
final case class Item(id: Long, name: String)
final case class Order(items: List[Item])

