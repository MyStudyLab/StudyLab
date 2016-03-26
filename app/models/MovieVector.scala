package models

import reactivemongo.bson.Macros

case class MovieVector(movies: Vector[Movie])

object MovieVector {

  implicit val MovieVectorReader = Macros.reader[MovieVector]

  implicit val MovieVectorWriter = Macros.writer[MovieVector]

}