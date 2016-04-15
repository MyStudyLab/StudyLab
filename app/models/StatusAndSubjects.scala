package models

import reactivemongo.bson.Macros

/**
  *
  * @param user_id
  * @param subjects
  * @param status
  */
case class StatusAndSubjects(user_id: Int, subjects: Vector[Subject], status: UserStatus)

object StatusAndSubjects {

  implicit val StatusDataWriter = Macros.writer[StatusAndSubjects]

  implicit val StatusDataReader = Macros.reader[StatusAndSubjects]

}