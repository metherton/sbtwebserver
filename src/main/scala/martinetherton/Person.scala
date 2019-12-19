package martinetherton

import java.sql.Timestamp

case class Person(firstName: String, surname: String, id: Option[Long] = None)
