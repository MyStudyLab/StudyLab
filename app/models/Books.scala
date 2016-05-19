package models

import constructs.{Book, BookVector}
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


  def getStatsAsJSON(user_id: Int): Future[Option[JsObject]] = {

    val selector = Json.obj("user_id" -> user_id)

    val projector = Json.obj("stats" -> 1, "status" -> 1, "_id" -> 0)

    jsonBooksCollection.find(selector, projector).one[JsObject]
  }


  def getBooks(user_id: Int): Future[Option[Vector[Book]]] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("_id" -> 0, "books" -> 1)

    bsonBooksCollection.find(selector, projector).one[BookVector].map(_.map(_.books))
  }


  def updateBooks(user_id: Int): Future[Boolean] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("_id" -> 0, "books" -> 1)

    // Get books for user
    bsonBooksCollection.find(selector, projector).one[BookVector].flatMap { optBookVector =>

      optBookVector.fold(Future(false))(bookVec => {

        // Construct the modifier
        val modifier = BSONDocument(
          "$set" -> BSONDocument(
            "stats" -> Books.stats(bookVec.books)
          )
        )

        // Update the book stats
        bsonBooksCollection.update(selector, modifier, multi = false).map(_.ok)
      })
    }
  }

}

object Books {

  def stats(books: Vector[Book]): BSONDocument = {


    BSONDocument(
      "authorCount" -> BSONInteger(authorCount(books)),
      "bookCount" -> BSONInteger(bookCount(books)),
      "pageCount" -> BSONInteger(imputedPageCount(books)),
      "averagePageCount" -> BSONInteger(averagePageCount(books)),
      "booksPerAuthor" -> BSONArray(booksPerAuthor(books).toVector.sortBy(p => -p._2).map(p => BSONArray(BSONString(p._1), BSONInteger(p._2)))),
      "pagesPerAuthor" -> BSONArray(pagesPerAuthor(books).toVector.sortBy(p => -p._2).map(p => BSONArray(BSONString(p._1), BSONInteger(p._2)))),
      "cumulativePages" -> BSONArray(cumulativePages(books).map(p => BSONArray(BSONLong(p._1), BSONInteger(p._2)))),
      "booksPerPubYear" -> BSONArray(booksPerPubYear(books).map(p => BSONArray(BSONInteger(p._1), BSONInteger(p._2))))
    )
  }

  def authorCount(books: Vector[Book]): Int = {

    books.flatMap(_.authors).toSet.size
  }


  def bookCount(books: Vector[Book]): Int = {

    books.length
  }


  def imputedPageCount(books: Vector[Book]): Int = {

    val hasPageCount = books.filter(_.pages > 0).map(_.pages)

    val avg = hasPageCount.sum / hasPageCount.length

    avg * books.length
  }


  def averagePageCount(books: Vector[Book]): Int = {

    val hasPageCount = books.filter(_.pages > 0).map(_.pages)

    val avg = hasPageCount.sum / hasPageCount.length

    avg
  }


  def booksPerAuthor(books: Vector[Book]): Map[String, Int] = {
    books.flatMap(_.authors).groupBy(a => a).mapValues(_.length)
  }


  // Only looking at first author. Could change that.
  def pagesPerAuthor(books: Vector[Book]): Map[String, Int] = {
    books.filter(_.authors.nonEmpty).groupBy(_.authors.head).mapValues(books => books.map(_.pages).sum)
  }

  def cumulativePages(books: Vector[Book]): Vector[(Long, Int)] = {

    val avg = averagePageCount(books)

    // First, impute missing page counts
    books.sortBy(_.finished).map(book => (book.finished, if (book.pages > 0) book.pages else avg)).map({

      var s = 0

      p => {
        s += p._2
        (p._1, s)
      }
    })

  }

  def booksPerPubYear(books: Vector[Book]): Vector[(Int, Int)] = {
    books.filter(_.pubYear != 0).groupBy(_.pubYear).mapValues(_.length).toVector.sortBy(_._1)
  }
}

