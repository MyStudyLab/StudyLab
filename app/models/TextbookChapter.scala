package models

import reactivemongo.bson.Macros


case class TextbookChapter(title: String, startPage: Int, endPage: Int, numSections: Int)


object TextbookChapter {

  implicit val textbookChapterReader = Macros.reader[TextbookChapter]

  implicit val textbookChapterWriter = Macros.writer[TextbookChapter]

}