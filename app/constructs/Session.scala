package constructs

import play.api.libs.json._


/**
  * Represents a study session.
  *
  * @param subject   The subject being studied.
  * @param startTime The time in milliseconds that the session began.
  * @param endTime   The time in milliseconds that the session ended.
  * @param message   The commit message for the session.
  */
case class Session(subject: String, startTime: Long, endTime: Long, message: String) {

  def durationHours(): Double = {
    (endTime - startTime).toDouble / 3600000
  }

  def durationMillis(): Long = {
    endTime - startTime
  }

}


object Session {

  import reactivemongo.bson.Macros

  // Implicitly convert to/from BSON
  implicit val sessionHandler = Macros.handler[Session]


  // Implicitly convert to JSON
  implicit val sessionWrites = Json.writes[Session]

}
