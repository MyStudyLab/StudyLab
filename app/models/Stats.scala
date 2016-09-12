package models

import java.time
import java.time.temporal.ChronoUnit
import java.time.{ZoneId, ZonedDateTime}

import constructs.{Session, Status}
import play.api.libs.json._


case class Stats(daysSinceStart: Long, currentStreak: Int, dailyAverage: Double, todaysTotal: Double, todayStart: Long,
                 todaysSessions: Vector[Session], probability: Vector[Double], movingAverage: Vector[(Long, Double)])

object Stats {

  // Implicitly convert to JSON
  implicit object StatsWrites extends Writes[Stats] {

    def writes(stats: Stats): JsValue = Json.obj(
      "daysSinceStart" -> stats.daysSinceStart,
      "currentStreak" -> stats.currentStreak,
      "dailyAverage" -> stats.dailyAverage,
      "todayStart" -> stats.todayStart,
      "todaysTotal" -> stats.todaysTotal,
      "todaysSessions" -> stats.todaysSessions,
      "probability" -> stats.probability,
      "movingAverage" -> stats.movingAverage.map(p => JsArray(Seq(JsNumber(p._1), JsNumber(p._2))))
    )

  }

  /**
    *
    * @param sessions The study sessions for which to compute statistics.
    * @return
    */
  def compute(sessions: Vector[Session], status: Status): Stats = {

    val currTimeMillis = System.currentTimeMillis()

    val zone = ZoneId.of("America/Chicago")

    val todayStart = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.DAYS).toInstant.toEpochMilli

    val totalHours = if (status.isStudying) {
      total(sessions) + (currTimeMillis - status.start) / 3600000d
    } else {
      total(sessions)
    }

    val dailyAverage = totalHours / daysSinceStart(zone)(sessions)

    val streak = currentStreak(sessions)

    val todaysSessionsVec = if (status.isStudying) {
      todaysSessions(zone)(sessions) :+ Session(status.subject, status.start, currTimeMillis, "")
    } else {
      todaysSessions(zone)(sessions)
    }

    val todaysTotal = total(todaysSessionsVec)

    Stats(daysSinceStart(zone)(sessions), streak, dailyAverage, todaysTotal, todayStart, todaysSessionsVec,
      probability(144)(sessions), movingAverage(15)(sessions)
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
    * The length of the user's current streak
    *
    * @param sessions The user's session list.
    * @return
    */
  private def currentStreak(sessions: Vector[Session]): Int = {

    val zone = ZoneId.of("America/Chicago")

    var current: Int = 0

    // The last element of dailyTotals always holds today's total
    val dTotals = dailyTotals(zone)(sessions)

    // We don't analyze today's total until later
    for (dailyTotal <- dTotals.dropRight(1)) {
      if (dailyTotal > 0.0) {
        current += 1
      } else {
        current = 0
      }
    }

    // Increment the last (current) streak if the user has programmed today
    dTotals.lastOption.fold(current)(last => {

      if (last > 0) {
        current += 1
      }

      current
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

    (bounds :+ (endDayZDT.toInstant.toEpochMilli, endInstant.toEpochMilli)).zip(groupSessions(sessions, dayMarks))
  }


  private def dailyTotals(zone: ZoneId)(sessions: Vector[Session]): Vector[Double] = {

    groupDays(zone)(sessions).map(s => total(s._2))
  }


  private def startDate(zone: ZoneId)(sessions: Vector[Session]): ZonedDateTime = {

    // TODO: check for empty lists
    val startInstant = time.Instant.ofEpochMilli(sessions.head.startTime)

    val startZDT = time.ZonedDateTime.ofInstant(startInstant, zone).truncatedTo(ChronoUnit.DAYS)

    startZDT
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