package models

import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONArray, BSONDocument, BSONInteger, BSONLong, BSONString}
import reactivemongo.play.json._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext


class Books(val api: ReactiveMongoApi) {

  def bsonBooksCollection: BSONCollection = api.db.collection[BSONCollection]("books")

  def jsonBooksCollection: JSONCollection = api.db.collection[JSONCollection]("books")


  def getBooks(user_id: Int): Future[Option[Vector[Book]]] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("_id" -> 0, "books" -> 1)

    bsonBooksCollection.find(selector, projector).one[BookVector].map(_.map(_.books))
  }


  def addBook(user_id: Int, book: Book): Future[Boolean] = {
    ???
  }
}
