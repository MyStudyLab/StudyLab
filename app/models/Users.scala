package models

import constructs.User
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
  * User model class.
  *
  * @param api Holds the reference to the database.
  */
class Users(val api: ReactiveMongoApi) {


  // Connection to the User collection
  def bsonUsersCollection: BSONCollection = api.db.collection[BSONCollection]("users")


  /**
    * Return true iff the username is already in the database.
    *
    * @param username The username to check for.
    * @return
    */
  def usernameInUse(username: String): Future[Boolean] = {

    val selector = BSONDocument("username" -> username)

    bsonUsersCollection.count(Some(selector), limit = 1).map(c => if (c == 0) false else true)
  }

  /**
    * Return true if the email is already in the database.
    *
    * @param email The email address to check for.
    * @return
    */
  def emailInUse(email: String): Future[Boolean] = {

    val selector = BSONDocument("email" -> email)

    bsonUsersCollection.count(Some(selector), limit = 1).map(c => if (c == 0) false else true)
  }

  /**
    * Add a new user to the database.
    *
    * @param newUser The user being added.
    * @return
    */
  def addNewUser(newUser: User): Future[Boolean] = {

    bsonUsersCollection.insert(newUser).map(result => {
      result.ok
    })
  }

  /**
    * Check a string against a user's password
    *
    * @param username The username for which to check the password.
    * @param given    The string to check against the user's actual password.
    * @return
    */
  def checkPassword(username: String, given: String): Future[Boolean] = {

    val selector = BSONDocument("username" -> username)

    // Only need to retrieve password
    val projector = BSONDocument("password" -> 1, "_id" -> 0)

    bsonUsersCollection.find(selector, projector).one[User].map(optUser =>

      optUser.fold(false)(user => user.password == given)
    )
  }


  def findByUsername(query: String): Future[List[User]] = {
    ???
  }

  def findByName(name: String): Future[Option[Int]] = {
    ???
  }

}
