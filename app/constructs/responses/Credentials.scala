package constructs.responses

import play.api.libs.json.{Json, Writes}
import reactivemongo.bson.BSONDocument


/**
  * The credential information stored for each user
  *
  * @param username The user's username
  * @param password The user's password
  */
case class Credentials(username: String, password: String) {

}

object Credentials {

  import reactivemongo.bson.Macros

  // Implicitly convert to/from BSON
  implicit val CredentialsHandler = Macros.handler[Credentials]


  val projector = BSONDocument("username" -> 1, "password" -> 1, "_id" -> 0)

  // Implicitly convert to JSON
  implicit val CredentialsWriter: Writes[Credentials] = Json.writes[Credentials]
}