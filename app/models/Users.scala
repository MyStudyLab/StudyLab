package models

// Standard Library
import scala.concurrent.Future

// Play Framework
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// Reactive Mongo
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument

// Project
import constructs.User
import constructs.responses.{ProfilesOnly, AboutMessage}
import helpers.Selectors.{emailSelector, usernameSelector}

/**
  * User model class.
  *
  * @param api Holds the reference to the database.
  */
class Users(val api: ReactiveMongoApi) {

  // The users collection
  protected def users: BSONCollection = api.db.collection[BSONCollection]("users")


  /**
    * Add a new user to the database.
    *
    * @param newUser The user being added.
    * @return
    */
  def addNewUser(newUser: User): Future[Boolean] = {

    users.insert(newUser).map(result => {
      result.ok
    })

  }

  /**
    * Get the about message for the user.
    *
    * @param username The username for which to retrieve the data.
    * @return
    */
  def aboutMessage(username: String): Future[Option[AboutMessage]] = {

    users.find(usernameSelector(username), AboutMessage.projector).one[AboutMessage]
  }

  /**
    * Get the social profiles for the user.
    *
    * @param username The username for which to retrieve data.
    * @return
    */
  def socialProfiles(username: String): Future[Option[ProfilesOnly]] = {

    users.find(usernameSelector(username), ProfilesOnly.projector).one[ProfilesOnly]
  }

  /**
    * Return true iff the username is already in the database.
    *
    * @param username The username to check for.
    * @return
    */
  def usernameInUse(username: String): Future[Boolean] = {

    users.count(Some(usernameSelector(username)), limit = 1).map(count => count != 0)
  }

  /**
    * Return true if the email is already in the database.
    *
    * @param email The email address to check for.
    * @return
    */
  def emailInUse(email: String): Future[Boolean] = {

    users.count(Some(emailSelector(email)), limit = 1).map(count => count != 0)
  }


  /**
    * Check a string against a user's password
    *
    * @param username The username for which to check the password.
    * @param given    The string to check against the user's actual password.
    * @return
    */
  def checkCredentials(username: String, given: String): Future[Boolean] = {

    users.find(usernameSelector(username), User.projector).one[User].map(optUser =>

      optUser.fold(false)(user => user.password == given)
    )
  }


  def getUserByUsername(username: String): Future[Option[User]] = {
    users.find(usernameSelector(username), User.projector).one[User]
  }

  /**
    * Search for a particular username
    *
    * @param query The search text
    * @param limit The maximum number of results to return
    * @return
    */
  def searchUsername(query: String, limit: Int = Int.MaxValue): Future[List[User]] = {

    // ALl usernames which contain the query
    val selector = BSONDocument("username" -> BSONDocument("$regex" -> query))

    users.find(selector).cursor[User]().collect[List](upTo = limit)
  }

  /**
    * Find a user by their actual name
    *
    * @param name
    * @return
    */
  def findByName(name: String): Future[Option[Int]] = {
    ???
  }

}
