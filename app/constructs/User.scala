package constructs

import reactivemongo.bson.Macros

/**
  * Represents a user of the app.
  *
  * @param user_id   A unique, identifying number used by the model layer.
  * @param firstName The first name of the user.
  * @param lastName  The last name of the user.
  * @param email     The email address of the user.
  * @param password  The account password for the user.
  * @param joined    The timestamp when the user joined.
  */
case class User(user_id: Int, firstName: String, lastName: String, email: String, password: String, joined: Long)

object User {

  // Implicitly converts from BSON
  implicit val UserReader = Macros.reader[User]

  // Implicitly converts to BSON
  implicit val UserWriter = Macros.writer[User]

}