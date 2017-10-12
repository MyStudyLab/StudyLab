package helpers

// Reactive Mongo
import reactivemongo.bson.{BSONDocument, BSONObjectID}


/**
  * A collection of helper functions to generate BSON selectors
  */
object Selectors {

  /**
    * Get a selector for the given value of the "username" field
    *
    * @param username The username by which to select.
    * @return
    */
  def usernameSelector(username: String) = BSONDocument("username" -> username)

  /**
    *
    * @param username
    * @param id
    * @return
    */
  def usernameAndID(username: String, id: BSONObjectID) = BSONDocument(
    "username" -> username,
    "_id" -> id
  )

  /**
    * Get a selector for the given value of the "email" field
    *
    * @param email The email by which to select.
    * @return
    */
  def emailSelector(email: String) = BSONDocument("contactInfo.email" -> email)

}
