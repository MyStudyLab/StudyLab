package models

import reactivemongo.bson.Macros


case class Book(title: String, authors: Vector[String], userRating: Int, finished: Long,
                pages: Int, isbn: String, isbn13: String)


object Book {

  implicit val BookReader = Macros.reader[Book]

  implicit val BookWriter = Macros.writer[Book]

}