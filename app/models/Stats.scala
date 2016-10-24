package models

import java.time
import java.time.temporal.ChronoUnit
import java.time.ZoneId

import constructs.{Session, Status}
import play.api.libs.json._


case class Stats(movingAverage: Vector[(Long, Double)])

object Stats {

  // Implicitly convert to JSON
  implicit object StatsWrites extends Writes[Stats] {

    def writes(stats: Stats): JsValue = Json.obj(
      "movingAverage" -> stats.movingAverage.map(p => JsArray(Seq(JsNumber(p._1), JsNumber(p._2))))
    )

  }

  /**
    *
    * @param sessions The study sessions for which to compute statistics.
    * @return
    */
  def compute(sessions: Vector[Session], status: Status): Stats = {

    Stats(movingAverage(15)(sessions)
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

}