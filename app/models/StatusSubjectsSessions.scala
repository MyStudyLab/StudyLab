package models

import constructs.{Session, Status, Subject}
import reactivemongo.bson.Macros


case class StatusSubjectsSessions(user_id: Int, status: Status, subjects: Vector[Subject], sessions: Vector[Session])


object StatusSubjectsSessions {

  // Implicitly converts to/from BSON
  implicit val SessionDataHandler = Macros.handler[StatusSubjectsSessions]

}
