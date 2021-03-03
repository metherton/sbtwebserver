package martinetherton.mappers

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import martinetherton.domain._
import spray.json.DefaultJsonProtocol

trait Marshallers extends DefaultJsonProtocol  with SprayJsonSupport {

  implicit val stockFormat = jsonFormat2(Stock)

//  implicit val itemFormat = jsonFormat2(Item)
//  implicit val orderFormat = jsonFormat1(Order)
//  implicit val itemVoFormat = jsonFormat2(ItemVo)

}
