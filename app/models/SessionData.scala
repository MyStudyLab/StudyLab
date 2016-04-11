package models

import reactivemongo.bson.Macros


case class SessionData(user_id: Int, subjects: Vector[String], status: UserStatus, sessions: Vector[Session])

object SessionData {

  implicit val SessionDataReader = Macros.reader[SessionData]

  implicit val SessionDataWriter = Macros.writer[SessionData]

}
