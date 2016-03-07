package models

import java.util.Date

import reactivemongo.bson.Macros

case class UserStatus(isStudying: Boolean, subject: String, start: Long)

object UserStatus {

  // TODO: Write custom reader to convert bsondatetime to long
  implicit val UserStatusReader = Macros.reader[UserStatus]

  implicit val UserStatusWriter = Macros.writer[UserStatus]

}
