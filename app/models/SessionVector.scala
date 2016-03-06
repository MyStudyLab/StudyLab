package models

import reactivemongo.bson.Macros

case class SessionVector(sessions: Vector[Session])

object SessionVector {

  implicit val sessionVectorReader = Macros.reader[SessionVector]

}
