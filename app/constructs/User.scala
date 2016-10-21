package constructs


/**
  * Represents a user of the app.
  *
  * @param username  A unique, identifying string used by the model layer.
  * @param firstName The first name of the user.
  * @param lastName  The last name of the user.
  * @param email     The email address of the user.
  * @param password  The account password for the user.
  * @param joined    The timestamp when the user joined.
  */
case class User(username: String, firstName: String, lastName: String, email: String, password: String, joined: Long)


object User {

  import reactivemongo.bson.Macros

  // Implicitly converts to/from BSON
  implicit val UserHandler = Macros.handler[User]

}