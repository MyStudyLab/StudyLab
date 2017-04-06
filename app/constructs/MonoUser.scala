package constructs

// Reactive Mongo
import reactivemongo.bson.BSONDocument


/**
  * Represents a user of the app.
  *
  * @param username    A unique, identifying string used by the model layer
  * @param about       A textual description of the user and their goals
  * @param contactInfo Contact information for the user
  * @param password    The account password for the user
  * @param joined      The timestamp when the user joined
  * @param status      The current status of the user
  * @param subjects    The valid study subjects for the user
  * @param sessions    The session list for this user
  */
case class MonoUser(username: String, about: String, contactInfo: ContactInfo, password: String, joined: Long,
                    status: Status, subjects: Vector[Subject], sessions: Vector[Session])


object MonoUser {

  import reactivemongo.bson.Macros

  // Implicitly convert to/from BSON
  implicit val userHandler = Macros.handler[MonoUser]

  // A MongoDB projector to get only the fields for this class
  val projector = BSONDocument("_id" -> 0)

  /**
    * Constructor useful when creating a new user
    *
    * @param username A unique, identifying string used by the model layer
    * @param email    The user's email address
    * @param password The account password for the user
    * @return
    */
  def apply(username: String, email: String, password: String): MonoUser = MonoUser(username, "", ContactInfo.onlyEmail(email),
    password, System.currentTimeMillis(), Status.empty, Vector[Subject](), Vector[Session]())
}