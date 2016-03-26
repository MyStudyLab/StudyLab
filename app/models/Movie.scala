package models

import reactivemongo.bson.Macros


case class Movie(title: String, directors: Vector[String],
                 releaseYear: Int, watched: Long,
                 runtime: Int, userRating: Int)

object Movie {

  implicit val MovieReader = Macros.reader[Movie]

  implicit val MovieWriter = Macros.writer[Movie]

}