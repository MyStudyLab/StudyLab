package constructs


case class Textbook(title: String, authors: List[String], chapters: Vector[TextbookChapter])


object Textbook {

  import reactivemongo.bson.Macros

  // Implicitly converts to/from BSON
  implicit val textbookHandler = Macros.handler[Textbook]

}
