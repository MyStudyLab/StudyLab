package models

import play.api.libs.json._
import reactivemongo.bson._


/**
  * Represents a study session.
  *
  * @param subject   The subject being studied.
  * @param startTime The time in milliseconds that the session began.
  * @param endTime   The time in milliseconds that the session ended.
  */
case class Session(subject: String, startTime: Long, endTime: Long) {

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

  implicit val sessionWrites = Json.writes[Session]

}
