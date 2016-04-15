package models

import reactivemongo.bson.Macros

case class User(user_id: Int, password: String, name: String, joined: Long)

object User {

  implicit val UserReader = Macros.reader[User]

  implicit val UserWriter = Macros.writer[User]

}