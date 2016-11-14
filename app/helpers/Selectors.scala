package helpers

import reactivemongo.bson.BSONDocument


object Selectors {

  /**
    * Get a selector for the given value of the "username" field
    *
    * @param username The username by which to select.
    * @return
    */
  def usernameSelector(username: String) = BSONDocument("username" -> username)

  def emailSelector(email: String) = BSONDocument("email" -> email)

}
