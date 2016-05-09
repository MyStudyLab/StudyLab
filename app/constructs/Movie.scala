package constructs

import play.api.libs.json.JsObject
import reactivemongo.bson.Macros


case class Movie(title: String, directors: Vector[String],
                 actors: Vector[String], genres: Vector[String],
                 releaseYear: Int, watched: Long, runtime: Int, userRating: Int)

object Movie {

  // Implicitly converts to/from BSON
  implicit val MovieHandler = Macros.handler[Movie]


  def fromOMDB(json: JsObject, watched: Long, userRating: Int): Option[Movie] = {

    def splitOnComma(s: String): Vector[String] = {
      s.split(", ").toVector
    }


    for {
      title <- (json \ "Title").asOpt[String]
      directors <- (json \ "Director").asOpt[String]
      actors <- (json \ "Actors").asOpt[String]
      genres <- (json \ "Genre").asOpt[String]
      year <- (json \ "Year").asOpt[Int]
      runtime <- (json \ "Runtime").asOpt[String]
    } yield Movie(title, splitOnComma(directors), splitOnComma(actors), splitOnComma(genres), year, watched,
      runtime.stripSuffix(" min.").toInt, userRating)

  }

}