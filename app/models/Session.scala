package models

import java.util.Date

import reactivemongo.bson._


case class Session(startTime: Long, endTime: Long, subject: String)

object Session {

  // implicit val sessionReader = Macros.reader[Session]

  implicit object sessionReader extends BSONDocumentReader[Session] {

    def read(bson: BSONDocument): Session = {
      val optSession = for {
        startTime <- bson.getAs[BSONNumberLike]("startTime")
        endTime <- bson.getAs[BSONNumberLike]("endTime")
        subject <- bson.getAs[String]("subject")
      } yield Session(startTime.toLong, endTime.toLong, subject)

      optSession.get
    }
  }

  implicit val sessionWriter = Macros.writer[Session]

}
