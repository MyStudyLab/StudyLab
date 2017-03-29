package constructs.responses

// Play Framework
import play.api.libs.json.{Json, Writes}

// Reactive Mongo
import reactivemongo.bson.BSONDocument

/**
  * Get the about message for a username
  *
  * @param username The user's username.
  * @param about    The about message for the user.
  */
case class AboutMessage(username: String, about: String)

object AboutMessage {

  import reactivemongo.bson.Macros

  // Implicitly convert to/from BSON
  implicit val AboutHandler = Macros.handler[AboutMessage]


  val projector = BSONDocument("username" -> 1, "about" -> 1, "_id" -> 0)

  // Implicitly convert to JSON
  implicit val AboutMessageWriter: Writes[AboutMessage] = Json.writes[AboutMessage]

}
