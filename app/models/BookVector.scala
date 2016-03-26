package models

import reactivemongo.bson.Macros

case class BookVector(books: Vector[Book])

object BookVector {

  implicit val BookVectorReader = Macros.reader[BookVector]

  implicit val BookVectorWriter = Macros.writer[BookVector]

}
