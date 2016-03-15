package models

import java.time
import java.time.{Instant, ZonedDateTime, LocalTime, ZoneId}
import java.time.temporal.ChronoUnit

import reactivemongo.bson._

import scala.collection.mutable

case class Stats(total: Double)

// How to organize? One object, or a class for each stat?
// TODO: In normal use, will need to update stats given past values and a single session
// TODO: Optimize and combine the update functions to reduce repeated computation
// TODO: Idea: language for specifying partial results of a computation (every intermediate value should be accessible)
object Stats {

  // The list of available stats
  val stats: Map[String, Vector[Session] => BSONValue] = Map(
    "introMessage" -> introMessage,
    "subjectTotalsGoogle" -> subjectTotalsGoogle,
    "cumulativeGoogle" -> cumulativeGoogle,
    "averageSessionGoogle" -> averageSessionGoogle,
    "subjectCumulativeGoogle" -> subjectCumulativeGoogle,
    "probabilityGoogle" -> probabilityGoogle(100),
    "todaysSessionsGoogle" -> todaysSessionsGoogle
  )


  def total(sessions: Vector[Session]): Double = {
    sessions.foldLeft(0L)((total: Long, session: Session) => {
      total + session.durationMillis()
    }).toDouble / (3600 * 1000)
  }

  def totalBSON(sessions: Vector[Session]): BSONDouble = {
    BSONDouble(total(sessions))
  }

  def introMessage(sessions: Vector[Session]): BSONDocument = {

    val zone = ZoneId.of("America/Chicago")

    val totalHours = total(sessions)

    val dailyAverage = totalHours / daysSinceStart(zone)(sessions)

    val startZDT = startDate(zone)(sessions)

    val streaks = currentAndLongestStreaks(sessions)

    val todaysSessionsVec = todaysSessions(zone)(sessions)

    val todaysTotal = total(todaysSessionsVec)

    BSONDocument(
      "total" -> BSONDouble(totalHours),
      "start" -> BSONArray(BSONInteger(startZDT.getMonthValue), BSONInteger(startZDT.getDayOfMonth), BSONInteger(startZDT.getYear)),
      "dailyAverage" -> BSONDouble(dailyAverage),
      "currentStreak" -> BSONInteger(streaks._1),
      "longestStreak" -> BSONInteger(streaks._2),
      "daysSinceStart" -> BSONInteger(daysSinceStart(zone)(sessions).toInt),
      "todaysTotal" -> BSONDouble(todaysTotal),
      "todaysSessionsGoogle" -> BSONDocument(
        "columns" -> BSONArray(
          BSONArray(BSONString("string"), BSONString("Row Label")),
          BSONArray(BSONString("string"), BSONString("Bar Label")),
          BSONArray(BSONString("number"), BSONString("Start")),
          BSONArray(BSONString("number"), BSONString("Finish"))
        ),
        "rows" -> BSONArray(todaysSessionsVec.map(s => BSONArray(BSONString("What I've Done Today"), BSONString(s.subject), BSONLong(s.startTime), BSONLong(s.endTime))))
      )
    )
  }


  /**
    * The length of the user's longest programming streak and their current streak
    *
    * @param sessions The user's session list.
    * @return
    */
  def currentAndLongestStreaks(sessions: Vector[Session]): (Int, Int) = {

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


  def updatedTotal(oldTotal: Double, newSession: Session): Double = {
    oldTotal + newSession.durationHours()
  }


  def subjectTotals(sessions: Vector[Session]): Map[String, Double] = {

    val kv = sessions.foldLeft(Map[String, Long]())((totals, session) => {

      val previous = totals.getOrElse(session.subject, 0L)

      totals.updated(session.subject, previous + session.durationMillis())
    }).mapValues(total => total.toDouble / (3600 * 1000))

    kv
  }


  def updatedSubjectTotals(oldSubjectTotals: Map[String, Double], newSession: Session): Map[String, Double] = {

    val oldTotal: Double = oldSubjectTotals.getOrElse(newSession.subject, 0.0)

    oldSubjectTotals.updated(newSession.subject, oldTotal + newSession.durationHours())
  }


  def subjectTotalsGoogle(sessions: Vector[Session]): BSONDocument = {

    val sortedTotals = subjectTotals(sessions).toVector.sortBy(p => -p._2)

    BSONDocument(
      "columns" -> BSONArray(
        BSONArray(BSONString("string"), BSONString("Subject")),
        BSONArray(BSONString("number"), BSONString("Total Hours"))
      ),
      "rows" -> BSONArray(sortedTotals.map(p => BSONArray(p._1, p._2)))
    )
  }

  def cumulative(sessions: Vector[Session]): Vector[(Long, Double)] = {

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

  def cumulativeGoogle(sessions: Vector[Session]): BSONDocument = {

    val cumulatives = cumulative(sessions)

    BSONDocument(
      "columns" -> BSONArray(
        BSONArray(BSONString("date"), BSONString("Date")),
        BSONArray(BSONString("number"), BSONString("Cumulative Hours"))
      ),
      "rows" -> BSONArray(cumulatives.map(p => BSONArray(BSONLong(p._1), BSONDouble(p._2))))
    )
  }


  def averageSession(sessions: Seq[Session]): Map[String, Double] = {

    val subTotals = mutable.Map[String, (Long, Long)]()

    for (session <- sessions) {

      val prev = subTotals.getOrElse(session.subject, (0L, 0L))

      subTotals(session.subject) = (prev._1 + session.durationMillis(), prev._2 + 1)
    }

    subTotals.mapValues(pair => pair._1.toDouble / (pair._2 * 3600 * 1000)).toMap
  }


  def averageSessionGoogle(sessions: Seq[Session]): BSONDocument = {

    val subTotals = averageSession(sessions).toVector.sortBy(pair => -pair._2)

    BSONDocument(
      "columns" -> BSONArray(
        BSONArray(BSONString("string"), BSONString("Subject")),
        BSONArray(BSONString("number"), BSONString("Average Session Length"))
      ),
      "rows" -> BSONArray(subTotals.map(p => BSONArray(BSONString(p._1), BSONDouble(p._2))))
    )
  }


  // TODO: Still wanting a more elegant way to do this
  def subjectCumulative(sessions: Vector[Session]): (Vector[Long], Map[String, Vector[Double]]) = {

    val marks = monthMarksSince(ZoneId.of("America/Chicago"))(sessions.head.startTime)

    val step1: Map[String, Vector[Session]] = sessions.foldLeft(Map[String, Vector[Session]]())((acc, s) =>
      acc.updated(s.subject, acc.getOrElse(s.subject, Vector[Session]()) :+ s)
    )

    val step2: Map[String, Vector[Vector[Session]]] = step1.mapValues(subSessions => groupSessions(subSessions, marks))

    val step3: Map[String, Vector[Double]] = step2.mapValues(
      vec => vec.map(sessionGroup => sessionGroup.map(sess => sess.durationMillis()).sum.toDouble / (3600 * 1000))
    )

    // Cumulate the values. We drop the first (zero) element due to the way scanLeft works
    val step4: Map[String, Vector[Double]] = step3.mapValues(_.scanLeft(0.0)(_ + _).drop(1))

    (marks :+ System.currentTimeMillis(), step4)
  }


  def subjectCumulativeGoogle(sessions: Vector[Session]): BSONDocument = {

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
  def groupSessions(sessions: Vector[Session], marks: Iterable[Long]): Vector[Vector[Session]] = {

    val groups = marks.foldLeft(sessions, Vector[Vector[Session]]())((acc, next) => {

      val sp = acc._1.span(_.endTime < next)

      val rem = sp._2.headOption.fold((None: Option[Session], None: Option[Session]))(sess => {
        if (sess.startTime < next) {
          (Some(Session(sess.startTime, next, sess.subject)), Some(Session(next, sess.endTime, sess.subject)))
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
  def groupDays(zone: ZoneId)(sessions: Vector[Session]): Vector[((Long, Long), Vector[Session])] = {

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


  def dailyTotals(zone: ZoneId)(sessions: Vector[Session]): Vector[Double] = {

    groupDays(zone)(sessions).map(s => total(s._2))
  }

  def startDate(zone: ZoneId)(sessions: Vector[Session]): ZonedDateTime = {

    // TODO: check for empty lists
    val startInstant = time.Instant.ofEpochMilli(sessions.head.startTime)

    val startZDT = time.ZonedDateTime.ofInstant(startInstant, zone).truncatedTo(ChronoUnit.DAYS)

    startZDT
  }


  def startOfDay(zone: ZoneId)(epochMilli: Long): Long = {

    ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), zone).truncatedTo(ChronoUnit.DAYS).toInstant.toEpochMilli
  }


  def dayMarksSince(zone: ZoneId)(start: Long): Vector[Long] = {

    val startInstant = time.Instant.ofEpochMilli(start)

    val endInstant = time.Instant.now()

    val startDayZDT = ZonedDateTime.ofInstant(startInstant, zone).truncatedTo(ChronoUnit.DAYS)

    val diff = startDayZDT.until(ZonedDateTime.ofInstant(endInstant, zone), ChronoUnit.DAYS)

    val dayMarks = (for (i <- 0L to diff) yield startDayZDT.plusDays(i).toInstant.toEpochMilli).toVector

    dayMarks
  }

  def monthMarksSince(zone: ZoneId)(start: Long): Vector[Long] = {

    val startInstant = time.Instant.ofEpochMilli(start)

    val endInstant = time.Instant.now()

    val startDayZDT = ZonedDateTime.ofInstant(startInstant, zone).truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1)

    val diff = startDayZDT.until(ZonedDateTime.ofInstant(endInstant, zone), ChronoUnit.MONTHS)

    val monthMarks = (for (i <- 0L to diff) yield startDayZDT.plusMonths(i).toInstant.toEpochMilli).toVector

    monthMarks
  }

  def daysSinceStart(zone: ZoneId)(sessions: Vector[Session]): Long = {

    val now = ZonedDateTime.now(zone)

    // We include the current day, hence the + 1
    startDate(zone)(sessions).until(now, ChronoUnit.DAYS) + 1
  }


  def todaysSessions(zone: ZoneId)(sessions: Vector[Session]): Vector[Session] = {

    val startOfToday = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.DAYS).toInstant.toEpochMilli

    sessionsSince(startOfToday)(sessions)
  }


  def sessionsSince(since: Long)(sessions: Vector[Session]): Vector[Session] = {

    // Call reverse to get sessions back in chronological order
    val unsplitSessions = sessions.reverseIterator.takeWhile(s => s.endTime > since).toVector

    // Handle the case where a session spans midnight
    val first = unsplitSessions.lastOption.map(s => Session(math.max(s.startTime, since), s.endTime, s.subject))

    first.toVector ++ unsplitSessions.dropRight(1).reverse
  }


  def todaysSessionsGoogle(sessions: Vector[Session]): BSONDocument = {

    BSONDocument(
      "columns" -> BSONArray(
        BSONArray(BSONString("string"), BSONString("Row Label")),
        BSONArray(BSONString("string"), BSONString("Bar Label")),
        BSONArray(BSONString("number"), BSONString("Start")),
        BSONArray(BSONString("number"), BSONString("Finish"))
      ),
      "rows" -> BSONArray(todaysSessions(ZoneId.of("America/Chicago"))(sessions.toVector).map(s => BSONArray(BSONString("What I've Done Today"), BSONString(s.subject), BSONLong(s.startTime), BSONLong(s.endTime))))
    )

  }

  def probability(numBins: Int)(sessions: Vector[Session]): Vector[(LocalTime, Double)] = {

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

    // Currently, the groups are in UTC, so days start at 6:00:00
    // TODO: generalize this using a given ZoneId
    val zero = LocalTime.of(6, 0, 0)

    val stepSeconds: Long = (86400.0 / numBins).toLong

    val binTimes = for (i <- 0 until numBins) yield zero.plusSeconds(i * stepSeconds + stepSeconds / 2)

    // Normalize by number of days
    val (front, back) = binTimes.zip(bins.map(_ / boundsAndGroups.length)).span(p => p._1.getHour >= 6)

    // Rearrange so that google charts displays them correctly
    (back ++ front).toVector
  }

  def probabilityGoogle(numBins: Int)(sessions: Vector[Session]): BSONDocument = {

    val probs = probability(numBins)(sessions)


    BSONDocument(
      "columns" -> BSONArray(
        BSONArray(BSONString("timeofday"), BSONString("Time of Day")),
        BSONArray(BSONString("number"), BSONString("Probability"))
      ),
      "rows" -> BSONArray(probs.map(p => BSONArray(BSONArray(p._1.getHour, p._1.getMinute, p._1.getSecond), BSONDouble(p._2)))),
      "options" -> BSONDocument(
        "chart" -> BSONDocument(
          "title" -> "Probability That I'm Programming"
        ),
        "legend" -> BSONDocument(
          "position" -> "none"
        ),
        "colors" -> BSONArray("green"),
        "dataOpacity" -> 0.25
      )
    )
  }


}
