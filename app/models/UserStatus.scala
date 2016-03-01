package models

import java.util.Date

import reactivemongo.bson.Macros

case class UserStatus(isStudying: Boolean, subject: String, start: Date)

object UserStatus {

  implicit val UserStatusReader = Macros.reader[UserStatus]

  implicit val UserStatusWriter = Macros.writer[UserStatus]

}
