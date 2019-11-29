package martinetherton

final case class ItemVo(id: String, name: String)
final case class Item(id: String, name: String)
final case class Order(items: List[Item])

