package models

import reactivemongo.bson.Macros

case class User(user_id: Long, firstName: String, lastName: String, email: String, password: String, joined: Long)

object User {

  implicit val UserReader = Macros.reader[User]

  implicit val UserWriter = Macros.writer[User]

}