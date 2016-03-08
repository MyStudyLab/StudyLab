package models

import java.util.Date

import reactivemongo.bson.{BSONNumberLike, BSONDocument, BSONDocumentReader, Macros}

case class UserStatus(isStudying: Boolean, subject: String, start: Long)

object UserStatus {

  implicit object statusReader extends BSONDocumentReader[UserStatus] {

    def read(bson: BSONDocument): UserStatus = {
      val optStatus = for {
        isStudying <- bson.getAs[Boolean]("isStudying")
        subject <- bson.getAs[String]("subject")
        start <- bson.getAs[BSONNumberLike]("start")
      } yield UserStatus(isStudying, subject, start.toLong)

      optStatus.get
    }
  }

  implicit val UserStatusWriter = Macros.writer[UserStatus]

}
