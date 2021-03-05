package martinetherton.domain

object Url {
  // factory method
  def apply(pathParts: List[String], params: List[Tuple2[String, String]]): String = new Url().apply(pathParts, params)
}
class Url() extends Function2[List[String], List[Tuple2[String, String]], String]{
  override def apply(resourceList: List[String], params: List[Tuple2[String, String]]): String = (resourceList, params) match {
    case (resourceUrlPart, Nil) => Constants.BaseUrl + resourceUrlPart.mkString("/") + Constants.ApiExt
    case (resourceUrlPart, params) => Constants.BaseUrl + resourceUrlPart.mkString("/") + Constants.ApiExt + "&" + params.map(param => param._1 + "=" + param._2).mkString("&")
  }
}
