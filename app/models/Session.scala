package models

import reactivemongo.bson._


case class Session(startTime: Long, endTime: Long, subject: String) {

  def durationHours(): Double = {
    (endTime - startTime).toDouble / (3600 * 1000)
  }

  def durationMillis(): Long = {
    endTime - startTime
  }

}

object Session {

  implicit val sessionReader = Macros.reader[Session]

  implicit val sessionWriter = Macros.writer[Session]

}
