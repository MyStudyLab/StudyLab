package models

import reactivemongo.bson.Macros

/**
  * Used when both the user's status and subject list are needed.
  *
  * @param user_id  The user's id.
  * @param status   The status of the user.
  * @param subjects The valid subjects for the user
  */
case class StatusAndSubjects(user_id: Int, status: Status, subjects: Vector[Subject])

object StatusAndSubjects {

  implicit val StatusDataWriter = Macros.writer[StatusAndSubjects]

  implicit val StatusDataReader = Macros.reader[StatusAndSubjects]

}