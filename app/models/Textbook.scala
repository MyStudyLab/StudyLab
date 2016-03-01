package models


import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson.Macros


case class Textbook(title: String, authors: List[String], chapters: Vector[TextbookChapter])


object Textbook {

  def insert(t: Textbook): Unit = {

  }

  def find(title: String): List[Textbook] = {
    ???
  }

  implicit val textbookReader = Macros.reader[Textbook]

  implicit val textbookWriter = Macros.writer[Textbook]

}
