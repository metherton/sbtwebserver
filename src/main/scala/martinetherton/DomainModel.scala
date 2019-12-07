package martinetherton

class DomainModel() {
  case class ItemVo(id: Long, name: String)
  case class Item(id: Long, name: String)
  case class Order(items: List[Item])
}

