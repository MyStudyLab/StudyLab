package models

import reactivemongo.bson.Macros


case class StatusData(user_id: Int, subjects: List[String], status: UserStatus)

object StatusData {

  implicit val StatusDataWriter = Macros.writer[StatusData]

  implicit val StatusDataReader = Macros.reader[StatusData]

}