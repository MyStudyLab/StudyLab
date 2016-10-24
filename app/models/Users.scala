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
import helpers.Selectors.{emailSelector, usernameSelector}

/**
  * User model class.
  *
  * @param api Holds the reference to the database.
  */
class Users(val api: ReactiveMongoApi) {


  // Connection to the user collection
  protected def usersCollectionBSON: BSONCollection = api.db.collection[BSONCollection]("users")


  /**
    * Return true iff the username is already in the database.
    *
    * @param username The username to check for.
    * @return
    */
  def usernameInUse(username: String): Future[Boolean] = {

    usersCollectionBSON.count(Some(usernameSelector(username)), limit = 1).map(count => count != 0)
  }

  /**
    * Return true if the email is already in the database.
    *
    * @param email The email address to check for.
    * @return
    */
  def emailInUse(email: String): Future[Boolean] = {

    usersCollectionBSON.count(Some(emailSelector(email)), limit = 1).map(count => count != 0)
  }

  /**
    * Add a new user to the database.
    *
    * @param newUser The user being added.
    * @return
    */
  def addNewUser(newUser: User): Future[Boolean] = {

    usersCollectionBSON.insert(newUser).map(result => {
      result.ok
    })

    // TODO: Add a corresponding document to the sessions collection
  }

  /**
    * Check a string against a user's password
    *
    * @param username The username for which to check the password.
    * @param given    The string to check against the user's actual password.
    * @return
    */
  def checkCredentials(username: String, given: String): Future[Boolean] = {

    usersCollectionBSON.find(usernameSelector(username), User.projector).one[User].map(optUser =>

      optUser.fold(false)(user => user.password == given)
    )
  }


  def getUserByUsername(username: String): Future[Option[User]] = {
    usersCollectionBSON.find(usernameSelector(username), User.projector).one[User]
  }

  /**
    *
    *
    * @param query
    * @param limit
    * @return
    */
  def searchUsername(query: String, limit: Int = Int.MaxValue): Future[List[User]] = {

    // ALl usernames which contain the query
    val selector = BSONDocument("username" -> BSONDocument("$regex" -> query))

    usersCollectionBSON.find(selector).cursor[User]().collect[List](upTo = limit)
  }

  def findByName(name: String): Future[Option[Int]] = {
    ???
  }

}
