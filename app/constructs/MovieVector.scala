package constructs

import reactivemongo.bson.Macros

case class MovieVector(movies: Vector[Movie])

object MovieVector {

  // Implicitly converts to/from BSON
  implicit val MovieVectorHandler = Macros.handler[MovieVector]

}