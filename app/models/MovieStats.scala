package models


object MovieStats {



  def directorCount(movies: Vector[Movie]): Int = {

    movies.flatMap(_.directors).toSet.size
  }

}
