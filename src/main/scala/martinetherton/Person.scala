package martinetherton

import java.sql.Timestamp

case class Person(firstName: String, surname: String, dateOfBirth: Timestamp, address: String, city: String, country: String,  id: Option[Long] = None)