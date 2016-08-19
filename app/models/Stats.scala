package models

import java.time
import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId, ZonedDateTime}

import constructs.{Session, Status}
import play.api.libs.json._
import reactivemongo.bson._

import scala.collection.mutable


case class Stats(start: (Int, Int, Int), daysSinceStart: Long, total: Double, currentStreak: Int, longestStreak: Int,
                 dailyAverage: Double, todaysTotal: Double, todaysSessions: Vector[Session], cumulative: Vector[(Long, Double)],
                 subjectTotals: Map[String, Double], probability: Vector[Double], movingAverage: Vector[(Long, Double)],
                 averageSession: Map[String, Double]) {


}

object Stats {

  // Implicitly convert to BSON
  implicit object StatsWriter extends BSONDocumentWriter[Stats] {

    def write(stats: Stats): BSONDocument = {

      BSONDocument(
        "start" -> BSONArray(stats.start._1, stats.start._2, stats.start._3),
        "daysSinceStart" -> stats.daysSinceStart,
        "total" -> stats.total,
        "currentStreak" -> stats.currentStreak,
        "longestStreak" -> stats.longestStreak,
        "dailyAverage" -> stats.dailyAverage,
        "todaysTotal" -> stats.todaysTotal,
        "todaysSessions" -> stats.todaysSessions,
        "cumulative" -> stats.cumulative.map(p => BSONArray(p._1, p._2)),
        "subjectTotals" -> stats.subjectTotals.toVector.sortBy(p => -p._2).map(p => BSONArray(p._1, p._2)),
        "probability" -> stats.probability,
        "movingAverage" -> stats.movingAverage.map(p => BSONArray(p._1, p._2)),
        "averageSession" -> stats.averageSession.toVector.sortBy(p => -p._2).map(p => BSONArray(p._1, p._2))
      )
    }

  }

  // Implicitly convert to JSON
  implicit object StatsWrites extends Writes[Stats] {

    def writes(stats: Stats): JsValue = {

      Json.obj(
        "start" -> JsArray(Seq(JsNumber(stats.start._1), JsNumber(stats.start._2), JsNumber(stats.start._3))),
        "daysSinceStart" -> stats.daysSinceStart,
        "total" -> stats.total,
        "currentStreak" -> stats.currentStreak,
        "longestStreak" -> stats.longestStreak,
        "dailyAverage" -> stats.dailyAverage,
        "todaysTotal" -> stats.todaysTotal,
        "todaysSessions" -> stats.todaysSessions,
        "cumulative" -> stats.cumulative.map(p => JsArray(Seq(JsNumber(p._1), JsNumber(p._2)))),
        "subjectTotals" -> stats.subjectTotals.toVector.sortBy(p => -p._2).map(p => JsArray(Seq(JsString(p._1), JsNumber(p._2)))),
        "probability" -> stats.probability,
        "movingAverage" -> stats.movingAverage.map(p => JsArray(Seq(JsNumber(p._1), JsNumber(p._2)))),
        "averageSession" -> stats.averageSession.toVector.sortBy(p => -p._2).map(p => JsArray(Seq(JsString(p._1), JsNumber(p._2))))
      )
    }

  }

  /**
    *
    * @param sessions The study sessions for which to compute statistics.
    * @return
    */
  def compute(sessions: Vector[Session], status: Status): Stats = {

    val currTimeMillis = System.currentTimeMillis()

    val zone = ZoneId.of("America/Chicago")

    val totalHours = if (status.isStudying) {
      total(sessions) + (currTimeMillis - status.start) / 3600000d
    } else {
      total(sessions)
    }

    val dailyAverage = totalHours / daysSinceStart(zone)(sessions)

    val (currentStreak, longestStreak) = currentAndLongestStreaks(sessions)

    val todaysSessionsVec = if (status.isStudying) {
      todaysSessions(zone)(sessions) :+ Session(status.subject, status.start, currTimeMillis, "")
    } else {
      todaysSessions(zone)(sessions)
    }

    val todaysTotal = total(todaysSessionsVec)

    val startZDT = startDate(zone)(sessions)



    BSONDocument(
      "subjectCumulative" -> subjectCumulativeGoogle(sessions)
    )

    Stats((startZDT.getMonthValue, startZDT.getDayOfMonth, startZDT.getYear), daysSinceStart(zone)(sessions),
      totalHours, currentStreak, longestStreak, dailyAverage, todaysTotal, todaysSessionsVec, cumulative(sessions),
      subjectTotals(sessions), probability(144)(sessions), movingAverage(15)(sessions), averageSession(sessions)
    )
  }


  /**
    * Total duration of the sessions.
    *
    * @param sessions The vector of sessions to sum.
    * @return
    */
  private def total(sessions: Vector[Session]): Double = {
    sessions.foldLeft(0L)((total: Long, session: Session) => {
      total + session.durationMillis()
    }).toDouble / (3600 * 1000)
  }


  /**
    * Average duration of a session.
    *
    * @param sessions The vector of sessions to average.
    * @return
    */
  private def average(sessions: Vector[Session]): Double = {
    sessions.foldLeft(0L)((total: Long, session: Session) => {
      total + session.durationMillis()
    }).toDouble / (3600 * 1000 * sessions.length)
  }


  private def subjectTotals(sessions: Vector[Session]): Map[String, Double] = {

    val kv = sessions.foldLeft(Map[String, Long]())((totals, session) => {

      val previous = totals.getOrElse(session.subject, 0L)

      totals.updated(session.subject, previous + session.durationMillis())
    }).mapValues(total => total.toDouble / (3600 * 1000))

    kv
  }


  private def cumulative(sessions: Vector[Session]): Vector[(Long, Double)] = {

    val boundsAndGroups = groupDays(ZoneId.of("America/Chicago"))(sessions)

    val cumulatives = boundsAndGroups.map({

      var s: Double = 0.0

      bg => (bg._1._2, {
        s += total(bg._2)
        s
      })
    })

    cumulatives
  }

  /**
    * The length of the user's longest programming streak and their current streak
    *
    * @param sessions The user's session list.
    * @return
    */
  private def currentAndLongestStreaks(sessions: Vector[Session]): (Int, Int) = {

    val zone = ZoneId.of("America/Chicago")

    var longest: Int = 0
    var current: Int = 0

    // The last element of dailyTotals always holds today's total
    val dTotals = dailyTotals(zone)(sessions)

    // We don't analyze today's total until later
    for (dailyTotal <- dTotals.dropRight(1)) {
      if (dailyTotal > 0.0) {

        // The current streak continues
        current += 1
      } else {

        // Check if we have a new longest streak
        if (current > longest) {
          longest = current
        }

        // Reset the current streak counter
        current = 0
      }
    }

    // Increment the last (current) streak if the user has programmed today
    dTotals.lastOption.fold((current, longest))(last => {

      if (last > 0) {
        current += 1
      }

      if (current > longest) {
        longest = current
      }

      (current, longest)
    })
  }


  private def movingAverage(radius: Int)(sessions: Vector[Session]): Vector[(Long, Double)] = {

    val dailyTotals = groupDays(ZoneId.of("America/Chicago"))(sessions).map(p => (p._1._1, total(p._2)))

    val startIndex = radius
    val endIndex = dailyTotals.length - radius
    val windowSize = 1 + 2 * radius

    for (i <- Vector.range(startIndex, endIndex)) yield {
      (dailyTotals(i)._1, dailyTotals.slice(i - radius, i + radius).map(_._2).sum / windowSize)
    }
  }


  private def averageSession(sessions: Seq[Session]): Map[String, Double] = {

    val subTotals = mutable.Map[String, (Long, Long)]()

    for (session <- sessions) {

      val prev = subTotals.getOrElse(session.subject, (0L, 0L))

      subTotals(session.subject) = (prev._1 + session.durationMillis(), prev._2 + 1)
    }

    subTotals.mapValues(pair => pair._1.toDouble / (pair._2 * 3600 * 1000)).toMap
  }


  private def subjectCumulative(sessions: Vector[Session]): (Vector[Long], Map[String, Vector[Double]]) = {

    val marks = monthMarksSince(ZoneId.of("America/Chicago"))(sessions.head.startTime)

    val step1: Map[String, Vector[Session]] = sessions.groupBy(_.subject)

    val step2: Map[String, Vector[Vector[Session]]] = step1.mapValues(subSessions => groupSessions(subSessions, marks))

    val step3: Map[String, Vector[Double]] = step2.mapValues(groups => groups.map(sessionGroup => total(sessionGroup)))

    // Cumulate the values. We drop the first (zero) element due to the way scanLeft works
    val step4: Map[String, Vector[Double]] = step3.mapValues(_.scanLeft(0.0)(_ + _).drop(1))

    (marks :+ System.currentTimeMillis(), step4)
  }


  private def subjectCumulativeGoogle(sessions: Vector[Session]): BSONDocument = {

    val subjectCumulatives = subjectCumulative(sessions)

    val dates: Seq[Long] = subjectCumulatives._1

    val subjects = subjectCumulatives._2.keys.toSeq

    BSONDocument(
      "columns" -> BSONArray(
        BSONArray(BSONString("date"), BSONString("Date")) +: subjects.map(sub => BSONArray(BSONString("number"), BSONString(sub)))
      ),
      "rows" -> dates.indices.map(i => BSONArray(BSONLong(dates(i)) +: subjects.map(sub => BSONDouble(subjectCumulatives._2(sub)(i)))))
    )
  }


  // Split up a sessions list using a list of dates.
  private def groupSessions(sessions: Vector[Session], marks: Iterable[Long]): Vector[Vector[Session]] = {

    val groups = marks.foldLeft(sessions, Vector[Vector[Session]]())((acc, next) => {

      val sp = acc._1.span(_.endTime < next)

      val rem = sp._2.headOption.fold((None: Option[Session], None: Option[Session]))(sess => {
        if (sess.startTime < next) {
          (Some(Session(sess.subject, sess.startTime, next, sess.message)), Some(Session(sess.subject, next, sess.endTime, sess.message)))
        } else {
          (None, Some(sess))
        }
      })

      // The drop(1) is here so that we don't duplicate the head element
      (rem._2 ++: sp._2.drop(1), acc._2 :+ (sp._1 ++ rem._1))
    })

    // Should we append that last group or not?
    groups._2 :+ groups._1
  }


  // TODO: Generalize this to accept a temporal unit
  // TODO: Make it an iterator
  private def groupDays(zone: ZoneId)(sessions: Vector[Session]): Vector[((Long, Long), Vector[Session])] = {

    // Epoch second to instant
    // TODO: fails on empty vector
    val startInstant = time.Instant.ofEpochMilli(sessions.head.startTime)

    // Should use the current instant, not the last session
    val endInstant = time.Instant.now()

    // Get end of first day
    val startDayZDT = time.ZonedDateTime.ofInstant(startInstant, zone).truncatedTo(ChronoUnit.DAYS).plusDays(1)

    // Get start of last day
    val endDayZDT = time.ZonedDateTime.now(zone).truncatedTo(ChronoUnit.DAYS)

    val diff = startDayZDT.until(endDayZDT, ChronoUnit.DAYS)

    val dayMarks = for (i <- 0L to diff) yield startDayZDT.plusDays(i).toInstant.toEpochMilli

    val bounds = (for (i <- 0L to diff) yield (
      startDayZDT.plusDays(i - 1).toInstant.toEpochMilli,
      startDayZDT.plusDays(i).toInstant.toEpochMilli
      )).toVector

    (bounds :+(endDayZDT.toInstant.toEpochMilli, endInstant.toEpochMilli)).zip(groupSessions(sessions, dayMarks))
  }


  private def dailyTotals(zone: ZoneId)(sessions: Vector[Session]): Vector[Double] = {

    groupDays(zone)(sessions).map(s => total(s._2))
  }


  private def dailyTotalCounts(zone: ZoneId)(sessions: Vector[Session]): Vector[(Int, Int)] = {

    val days = dailyTotals(zone)(sessions)

    days.groupBy(dailyTotal => math.ceil(dailyTotal).toInt).mapValues(_.length).toVector
  }


  private def probabilityOfDailyTotal(zone: ZoneId)(sessions: Vector[Session]): Vector[(Int, Int)] = {

    dailyTotals(zone)(sessions).map(dailyTotal => (100 * dailyTotal / 24).toInt).groupBy(a => a).mapValues(_.length).toVector

  }


  private def startDate(zone: ZoneId)(sessions: Vector[Session]): ZonedDateTime = {

    // TODO: check for empty lists
    val startInstant = time.Instant.ofEpochMilli(sessions.head.startTime)

    val startZDT = time.ZonedDateTime.ofInstant(startInstant, zone).truncatedTo(ChronoUnit.DAYS)

    startZDT
  }


  private def startOfDay(zone: ZoneId)(epochMilli: Long): Long = {

    ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), zone).truncatedTo(ChronoUnit.DAYS).toInstant.toEpochMilli
  }


  private def dayMarksSince(zone: ZoneId)(start: Long): Vector[Long] = {

    val startInstant = time.Instant.ofEpochMilli(start)

    val endInstant = time.Instant.now()

    val startDayZDT = ZonedDateTime.ofInstant(startInstant, zone).truncatedTo(ChronoUnit.DAYS)

    val diff = startDayZDT.until(ZonedDateTime.ofInstant(endInstant, zone), ChronoUnit.DAYS)

    val dayMarks = (for (i <- 0L to diff) yield startDayZDT.plusDays(i).toInstant.toEpochMilli).toVector

    dayMarks
  }


  private def monthMarksSince(zone: ZoneId)(start: Long): Vector[Long] = {

    val startInstant = time.Instant.ofEpochMilli(start)

    val endInstant = time.Instant.now()

    val startDayZDT = ZonedDateTime.ofInstant(startInstant, zone).truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1)

    val diff = startDayZDT.until(ZonedDateTime.ofInstant(endInstant, zone), ChronoUnit.MONTHS)

    val monthMarks = (for (i <- 0L to diff) yield startDayZDT.plusMonths(i).toInstant.toEpochMilli).toVector

    monthMarks
  }


  private def daysSinceStart(zone: ZoneId)(sessions: Vector[Session]): Long = {

    val now = ZonedDateTime.now(zone)

    // We include the current day, hence the + 1
    startDate(zone)(sessions).until(now, ChronoUnit.DAYS) + 1
  }


  private def todaysSessions(zone: ZoneId)(sessions: Vector[Session]): Vector[Session] = {

    val startOfToday = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.DAYS).toInstant.toEpochMilli

    sessionsSince(startOfToday)(sessions)
  }


  private def sessionsSince(since: Long)(sessions: Vector[Session]): Vector[Session] = {

    // Call reverse to get sessions back in chronological order
    val unsplitSessions = sessions.reverseIterator.takeWhile(s => s.endTime > since).toVector

    // Handle the case where a session spans midnight
    val first = unsplitSessions.lastOption.map(s => Session(s.subject, math.max(s.startTime, since), s.endTime, s.message))

    first.toVector ++ unsplitSessions.dropRight(1).reverse
  }


  private def probability(numBins: Int)(sessions: Vector[Session]): Vector[Double] = {

    val bins = Array.fill[Double](numBins)(0)

    val boundsAndGroups = groupDays(ZoneId.of("America/Chicago"))(sessions)

    for ((bounds, group) <- boundsAndGroups) {

      for (session <- group) {

        val diff: Long = bounds._2 - bounds._1

        val startBin: Int = (((session.startTime - bounds._1) * numBins) / diff).toInt

        val endBin: Int = (((session.endTime - bounds._1) * numBins) / diff).toInt

        // Currently excluding the end bin
        for (bin <- startBin until endBin) {
          bins(bin) += 1
        }
      }
    }

    // Normalize by number of days
    bins.toVector.map(_ / boundsAndGroups.length)
  }

}