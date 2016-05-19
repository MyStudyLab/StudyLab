package constructs


case class Book(title: String, authors: Vector[String], userRating: Int, finished: Long,
                pages: Int, isbn: String, isbn13: String, pubYear: Int, imageURL: String)


object Book {

  import reactivemongo.bson.Macros

  // Implicitly converts to/from BSON
  implicit val BookHandler = Macros.handler[Book]

}