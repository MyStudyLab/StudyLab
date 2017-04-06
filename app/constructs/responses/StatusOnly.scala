package constructs.responses

import constructs.{Projector, Status}
import play.api.libs.json.Json
import reactivemongo.bson.BSONDocument

/**
  * Used when both the user's status and subject list are needed.
  *
  * @param username The user's username.
  * @param status   The status of the user.
  */
case class StatusOnly(username: String, status: Status)


object StatusOnly {

  import reactivemongo.bson.Macros

  // Implicitly convert to/from BSON
  implicit val handler = Macros.handler[StatusOnly]

  // Implicitly convert to JSON
  implicit val writer = Json.writes[StatusOnly]

  implicit val projector = new Projector[StatusOnly] {
    val projector = BSONDocument("username" -> 1, "status" -> 1, "_id" -> 0)
  }
}