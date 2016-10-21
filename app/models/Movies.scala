package models

import constructs.{Movie, MovieVector}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONArray, BSONDocument, BSONInteger, BSONLong, BSONString}
import reactivemongo.play.json._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
  *
  * @param api
  */
class Movies(val api: ReactiveMongoApi) {


  def movieCollection: BSONCollection = api.db.collection[BSONCollection]("movies")

  def jsonMovieCollection: JSONCollection = api.db.collection[JSONCollection]("movies")


  def getAll(username: String): Future[Option[Vector[Movie]]] = {

    // Will look for the user with the given id
    val selector = BSONDocument("username" -> username)

    // Will get book data here
    val projector = BSONDocument("_id" -> 0, "movies" -> 1)

    // Get books for user
    movieCollection.find(selector, projector).one[MovieVector].map(_.map(_.movies))
  }


  def getAllJson(username: String): Future[Option[JsObject]] = {

    // Will look for the user with the given id
    val selector = Json.obj("username" -> username)

    // Will get book data here
    val projector = Json.obj("_id" -> 0, "movies" -> 1, "stats" -> 1)

    // Get books for user
    jsonMovieCollection.find(selector, projector).one[JsObject]
  }


  def updateStats(username: String): Future[Boolean] = {

    // Will look for the user with the given id
    val selector = BSONDocument("username" -> username)

    // Will get book data here
    val projector = BSONDocument("_id" -> 0, "movies" -> 1)

    // Get books for user
    movieCollection.find(selector, projector).one[MovieVector].flatMap { optMovieVector =>

      optMovieVector.fold(Future(false))(movieVec => {

        // Construct the modifier
        val modifier = BSONDocument(
          "$set" -> BSONDocument(
            "stats" -> Movies.stats(movieVec.movies)
          )
        )

        // Update the movie stats
        movieCollection.update(selector, modifier, multi = false).map(_.ok)
      })
    }
  }
}

/**
  *
  */
object Movies {

  def stats(movieVec: Vector[Movie]): BSONDocument = {

    BSONDocument(
      "movieCount" -> movieCount(movieVec),
      "directorCount" -> directorCount(movieVec),
      "actorCount" -> actorCount(movieVec),
      "moviesPerDirector" -> moviesPerDirector(movieVec).toVector.sortBy(p => -p._2).map(p => BSONArray(BSONString(p._1), BSONInteger(p._2))),
      "cumulativeMovies" -> cumulativeMovies(movieVec).map(p => BSONArray(BSONLong(p._1), BSONInteger(p._2))),
      "moviesPerReleaseYear" -> BSONArray(moviesPerReleaseYear(movieVec).map(p => BSONArray(BSONInteger(p._1), BSONInteger(p._2)))),
      "moviesPerGenre" -> moviesPerGenre(movieVec).toVector.sortBy(p => -p._2).map(p => BSONArray(BSONString(p._1), BSONInteger(p._2))),
      "moviesPerActor" -> moviesPerActor(movieVec).toVector.sortBy(p => -p._2).map(p => BSONArray(BSONString(p._1), BSONInteger(p._2)))
    )

  }


  private def movieCount(movieVec: Vector[Movie]): Int = {
    movieVec.length
  }


  private def directorCount(movieVec: Vector[Movie]): Int = {

    movieVec.flatMap(_.directors).toSet.size
  }


  private def actorCount(movieVec: Vector[Movie]): Int = {

    movieVec.flatMap(_.actors).toSet.size
  }


  private def moviesPerDirector(movieVec: Vector[Movie]): Map[String, Int] = {

    movieVec.flatMap(_.directors).groupBy(a => a).mapValues(_.length)
  }


  private def moviesPerActor(movieVec: Vector[Movie]): Map[String, Int] = {

    movieVec.flatMap(_.actors).groupBy(a => a).mapValues(_.length)
  }


  private def cumulativeMovies(movieVec: Vector[Movie]): Vector[(Long, Int)] = {

    movieVec.sortBy(_.watched).map({

      var s = 0

      p => {
        s += 1
        (p.watched, s)
      }
    })

  }


  private def moviesPerReleaseYear(movieVec: Vector[Movie]): Vector[(Int, Int)] = {
    movieVec.filter(_.releaseYear != 0).groupBy(_.releaseYear).mapValues(_.length).toVector.sortBy(_._1)
  }


  private def moviesPerGenre(movieVec: Vector[Movie]): Map[String, Int] = {

    movieVec.flatMap(_.genres).groupBy(a => a).mapValues(_.length)

  }

}