package models

import java.util.Date

import reactivemongo.bson.{BSONDocument, BSONDocumentWriter, Macros}


case class Session(startTime: Date, endTime: Date, subject: String)

object Session {

  implicit val sessionReader = Macros.reader[Session]

  implicit val sessionWriter = Macros.writer[Session]

}
