package constructs

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
  */
case class User(username: String, about: String, contactInfo: ContactInfo, password: String, joined: Long,
                status: Status, subjects: Vector[Subject])


object User {

  import reactivemongo.bson.Macros

  // Implicitly convert to/from BSON
  implicit val UserHandler = Macros.handler[User]

  // A MongoDB projector to get only the fields for this class
  val projector = BSONDocument(
    "username" -> 1, "about" -> 1, "contactInfo" -> 1, "password" -> 1, "joined" -> 1,
    "status" -> 1, "subjects" -> 1, "_id" -> 0
  )
}