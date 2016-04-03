package models

import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class Users(val api: ReactiveMongoApi) {

  def bsonUsersCollection: BSONCollection = api.db.collection[BSONCollection]("users")


  def checkPassword(user_id: Int, password: String): Future[Boolean] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("sessions" -> 0, "_id" -> 0)

    val futOptUser = bsonUsersCollection.find(selector, projector).one[User]

    futOptUser.flatMap(optUser =>

      optUser.fold(Future(false))((user: User) => Future(user.password == password))
    )
  }

}
