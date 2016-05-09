package models

import constructs.User
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
  *
  * @param api Holds the reference to the database.
  */
class Users(val api: ReactiveMongoApi) {


  def bsonUsersCollection: BSONCollection = api.db.collection[BSONCollection]("users")


  /**
    * Get a new and unique user ID.
    *
    * @return
    */
  def getNewUserID(): Future[Long] = {

    val trial: Long = (math.random * 1000000000L).toLong

    val selector = BSONDocument("user_id" -> trial)

    val projector = BSONDocument("_id" -> 0)

    bsonUsersCollection.find(selector, projector).one[User].flatMap(_.fold(Future(trial))(user => getNewUserID()))
  }


  /**
    *
    * @param firstName The first name of the new user.
    * @param lastName  The last name of the new user.
    * @param email
    * @param password
    * @return
    */
  def addNewUser(firstName: String, lastName: String, email: String, password: String): Future[Boolean] = {

    getNewUserID().flatMap(newUserID => {

      val newUser = User(0, firstName, lastName, email, password, System.currentTimeMillis())

      bsonUsersCollection.insert(newUser).map(result => {
        result.ok
      })
    })
  }

  /**
    * Check a string against a user's password
    *
    * @param user_id The user ID for which to check the password.
    * @param given   The string to check against the user's actual password.
    * @return
    */
  def checkPassword(user_id: Int, given: String): Future[Boolean] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("_id" -> 0)

    bsonUsersCollection.find(selector, projector).one[User].map(optUser =>

      optUser.fold(false)(user => user.password == given)
    )
  }


  def findUserByName(name: String): Future[Option[Int]] = {
    ???
  }

}
