package models

import reactivemongo.bson.Macros

case class UserWithSessions(user_id: Int, password: String, name: String, subjects: List[String],
                            status: UserStatus, sessions: Vector[Session])

object UserWithSessions {

  implicit val userWithSessionsReader = Macros.reader[UserWithSessions]

}