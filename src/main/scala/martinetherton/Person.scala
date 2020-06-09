package martinetherton

import java.sql.Timestamp

//case class Person(firstName: String, surname: String, dateOfBirth: Timestamp, address: String, city: String, country: String,  id: Option[Long] = None)


//case class Person(name: String, dateOfBirth: String, place: String, id: String)
case class Person(id: String, firstName: String, surname: String, dateOfBirth: String, place: String, dateOfDeath: String, placeOfDeath: String, sex: String, childRelation: List[String], parentRelation: String)
