package constructs

import reactivemongo.bson.Macros

/**
  * Represents the study status of a user.
  *
  * @param isStudying True iff the user is in a study session.
  * @param subject    If studying, the subject of the current session.
  * @param start      If studying, the start time in milliseconds of the current session.
  */
case class Status(isStudying: Boolean, subject: String, start: Long)


object Status {

  // Implicitly converts to/from BSON
  implicit val StatusHandler = Macros.handler[Status]

}
