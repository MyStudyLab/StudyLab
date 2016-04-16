package models

import java.time.LocalTime

import reactivemongo.bson.{BSONArray, BSONDocument, BSONDouble, BSONReader, BSONWriter}


case class Stats(total: Int, currentStreak: Int, longestStreak: Int,
                 lastUpdated: Long, dailyAverage: Double, cumulative: Vector[(Long, Double)],
                 subjectTotals: Map[String, Double], probability: Vector[(LocalTime, Double)],
                 slidingAverage: Vector[(Long, Double)], averageSession: Map[String, Double]) {

  def updated(s: Session): Stats = {

    ???
  }

}

object Stats {

  implicit object StatsReader extends BSONReader[BSONDocument, Stats] {

    def read(doc: BSONDocument): Stats = {

      val opt = for {
        total <- doc.getAs[Int]("total")
        currentStreak <- doc.getAs[Int]("currentStreak")
        longestStreak <- doc.getAs[Int]("longestStreak")
        lastUpdated <- doc.getAs[Long]("lastUpdated")
        dailyAverage <- doc.getAs[Double]("dailyAverage")
        cumulative <- doc.getAs[BSONArray]("cumulative").map(arr => arr.stream.toVector)
      } yield Stats(total, currentStreak, longestStreak, lastUpdated, dailyAverage, Vector(), Map(), Vector(), Vector(), Map())

      opt.get
    }

  }

  implicit object StatsWriter extends BSONWriter[Stats, BSONDocument] {

    def write(stats: Stats): BSONDocument = {

      BSONDocument(
        "total" -> stats.total,
        "dailyAverage" -> stats.dailyAverage,
        "currentStreak" -> stats.currentStreak,
        "longestStreak" -> stats.longestStreak,
        "lastUpdated" -> stats.lastUpdated,
        "cumulative" -> stats.cumulative.map(p => BSONArray(p._1, p._2)),
        "subjectTotals" -> stats.subjectTotals.toVector.map(p => BSONArray(p._1, p._2)),
        "probability" -> stats.probability.map(p => BSONArray(BSONArray(p._1.getHour, p._1.getMinute, p._1.getSecond), BSONDouble(p._2))),
        "slidingAverage" -> stats.slidingAverage.map(p => BSONArray(p._1, p._2)),
        "averageSession" -> stats.averageSession.toVector.sortBy(p => -p._2).map(p => BSONArray(p._1, p._2))
      )
    }

  }

}