package constructs

import reactivemongo.bson.Macros


case class TextbookChapter(title: String, startPage: Int, endPage: Int, numSections: Int)


object TextbookChapter {

  // Implicitly converts to/from BSON
  implicit val textbookChapterHandler = Macros.handler[TextbookChapter]

}