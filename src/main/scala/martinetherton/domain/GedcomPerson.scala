package martinetherton.domain

//case class Person(name: String, dateOfBirth: String, place: String, id: String)
case class GedcomPerson(id: Option[String], firstName: Option[String], surname: Option[String], dateOfBirth: Option[String], place: Option[String], dateOfDeath: Option[String], placeOfDeath: Option[String], sex: Option[String], childRelation: Option[List[String]], parentRelation: Option[String] )
