package models

import reactivemongo.bson.Macros

case class UserStatus(isStudying: Boolean, subject: String, start: Long)

object UserStatus {

  implicit val UserStatusReader = Macros.reader[UserStatus]

  implicit val UserStatusWriter = Macros.writer[UserStatus]

}
