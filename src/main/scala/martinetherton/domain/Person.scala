package martinetherton.domain

import java.sql.Timestamp

case class Person(firstName: String, surname: String, dateOfBirth: Timestamp, address: String, city: String, country: String,  id: Option[Long] = None, personId: Long, fatherId: Long, motherId: Long, childRelations: String, parentRelation: String, sex: String, tree: String)
