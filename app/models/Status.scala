package models

import reactivemongo.bson.Macros

case class Status(isStudying: Boolean, subject: String, start: Long)

object Status {

  implicit val UserStatusReader = Macros.reader[Status]

  implicit val UserStatusWriter = Macros.writer[Status]

}
