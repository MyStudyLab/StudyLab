package models

import reactivemongo.bson.Macros

case class SessionList(sessions: List[Session])

object SessionList {

  implicit val sessionListReader = Macros.reader[SessionList]

}
