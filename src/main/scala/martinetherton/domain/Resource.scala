package martinetherton.domain

object Resource {
  // factory method
  def apply(resourceParts: List[String]): String = new Resource().apply(resourceParts)
}
class Resource extends Function1[List[String], String] {
  override def apply(resourceParts: List[String]): String = resourceParts.mkString("/")
}
