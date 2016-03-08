package models

import reactivemongo.bson._


case class Session(startTime: Long, endTime: Long, subject: String)

object Session {

  implicit val sessionReader = Macros.reader[Session]

  implicit val sessionWriter = Macros.writer[Session]

}
