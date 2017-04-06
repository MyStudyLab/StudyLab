package constructs

import play.api.libs.json.Json


/**
  * Represents the study status of a user.
  *
  * @param isStudying True iff the user is in a study session.
  * @param subject    If studying, the subject of the current session.
  * @param start      If studying, the start time in milliseconds of the current session.
  */
case class Status(isStudying: Boolean, subject: String, start: Long)


object Status {

  import reactivemongo.bson.Macros

  // Implicitly convert to/from BSON
  implicit val StatusHandler = Macros.handler[Status]

  // Implicitly convert to JSON
  implicit val statusWrites = Json.writes[Status]


  /**
    * An empty status object, used when creating a new user
    */
  val empty: Status = Status(isStudying = false, "", 0L)

}
