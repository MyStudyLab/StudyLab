package models

import play.api.libs.json._
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

  /**
    *
    * @return
    */
  def bsonUsersCollection: BSONCollection = api.db.collection[BSONCollection]("users")


  /**
    * Check a string against a user's password
    *
    * @param user_id The user ID for which to check the password.
    * @param given   The string to check against the user's actual password.
    * @return
    */
  def checkPassword(user_id: Int, given: String): Future[Boolean] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("sessions" -> 0, "_id" -> 0)

    bsonUsersCollection.find(selector, projector).one[User].map(optUser =>

      optUser.fold(false)(user => user.password == given)
    )
  }


  def findUserByName(name: String): Future[Option[Int]] = {
    ???
  }

}
