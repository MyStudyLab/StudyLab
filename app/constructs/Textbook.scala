package constructs

import reactivemongo.bson.Macros


case class Textbook(title: String, authors: List[String], chapters: Vector[TextbookChapter])


object Textbook {

  def insert(t: Textbook): Unit = {

  }

  def find(title: String): List[Textbook] = {
    ???
  }

  // Implicitly converts to/from BSON
  implicit val textbookHandler = Macros.handler[Textbook]

}
