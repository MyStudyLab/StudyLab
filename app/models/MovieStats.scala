package models

import reactivemongo.bson.{BSONArray, BSONDocument, BSONInteger, BSONLong, BSONString}


object MovieStats {

  def stats(movieVec: MovieVector): BSONDocument = {

    BSONDocument(
      "movieCount" -> movieCount(movieVec),
      "directorCount" -> directorCount(movieVec),
      "moviesPerDirector" -> moviesPerDirector(movieVec).toVector.sortBy(p => -p._2).map(p => BSONArray(BSONString(p._1), BSONInteger(p._2))),
      "cumulativeMovies" -> cumulativeMovies(movieVec).map(p => BSONArray(BSONLong(p._1), BSONInteger(p._2))),
      "moviesPerReleaseYear" -> BSONArray(moviesPerReleaseYear(movieVec).map(p => BSONArray(BSONInteger(p._1), BSONInteger(p._2))))
    )

  }

  def movieCount(movieVec: MovieVector): Int = {
    movieVec.movies.length
  }

  def directorCount(movieVec: MovieVector): Int = {

    movieVec.movies.flatMap(_.directors).toSet.size
  }

  def moviesPerDirector(movieVec: MovieVector): Map[String, Int] = {

    movieVec.movies.flatMap(_.directors).groupBy(a => a).mapValues(_.length)
  }

  def cumulativeMovies(movieVec: MovieVector): Vector[(Long, Int)] = {

    // First, impute missing page counts
    movieVec.movies.sortBy(_.watched).map({

      var s = 0

      p => {
        s += 1
        (p.watched, s)
      }
    })

  }

  def moviesPerReleaseYear(movieVec: MovieVector): Vector[(Int, Int)] = {
    movieVec.movies.filter(_.releaseYear != 0).groupBy(_.releaseYear).mapValues(_.length).toVector.sortBy(_._1)
  }

}
