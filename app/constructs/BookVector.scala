package constructs

import reactivemongo.bson.Macros

case class BookVector(books: Vector[Book])

object BookVector {

  // Implicitly converts to/from BSON
  implicit val BookVectorHandler = Macros.handler[BookVector]

}
