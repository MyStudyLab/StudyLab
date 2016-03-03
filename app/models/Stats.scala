package models

import reactivemongo.bson._

import java.time.LocalDateTime

import java.util.Date

import scala.collection.mutable

// How to organize? One object, or a class for each stat?
object Stats {

  // The list of available stats
  val stats: Map[String, Seq[Session] => BSONValue] = Map(
    "total" -> total,
    "subjectTotals" -> subjectTotals,
    "cumulative" -> cumulative,
    "averageSession" -> averageSession,
    "subjectCumulative" -> subjectCumulative
  )


  def total(sessions: Seq[Session]): BSONDouble = {
    BSONDouble(sessions.foldLeft(0L)((total: Long, session: Session) => {
      total + (session.endTime.getTime - session.startTime.getTime)
    }).toDouble / (3600 * 1000))
  }


  // For the applicable stats, have such a function
  def updatedTotal(oldTotal: BSONDouble, newSession: Session): BSONDouble = {
    BSONDouble(oldTotal.value + (newSession.endTime.getTime - newSession.startTime.getTime).toDouble / (3600 * 1000))
  }


  def subjectTotals(sessions: Seq[Session]): BSONDocument = {

    val kv = sessions.foldLeft(Map[String, Long]())((totals, session) => {

      val previous = totals.getOrElse(session.subject, 0L)

      totals.updated(session.subject, previous + (session.endTime.getTime - session.startTime.getTime))
    }).toVector.sortBy(pair => -pair._2)

    BSONDocument(
      "keys" -> BSONArray(kv.map(pair => BSONString(pair._1))),
      "values" -> BSONArray(kv.map(pair => BSONDouble(pair._2.toDouble / (3600 * 1000))))
    )
  }


  def cumulative(sessions: Seq[Session]): BSONDocument = {

    // Use seconds since epoch for marks?
    val marks = (for (year <- 115 until 116; month <- 0 until 12) yield new Date(year, month, 1)) ++ Seq(new Date(116, 0, 1), new Date(116, 1, 1))

    val cumulatives = groupSessions(sessions, marks).map(
      sessionGroup => sessionGroup.map(sess => sess.endTime.getTime - sess.startTime.getTime).sum.toDouble / (3600 * 1000)
    ).foldLeft((0.0, Seq[Double]()))((acc, next) => (acc._1 + next, acc._2 :+ (acc._1 + next)))._2

    BSONDocument(
      "dates" -> marks.:+(new Date()),
      "values" -> cumulatives.map(tot => BSONDouble(tot))
    )
  }

  def averageSession(sessions: Seq[Session]): BSONDocument = {

    val subTotals = mutable.Map[String, (Long, Long)]()

    for (session <- sessions) {

      val prev = subTotals.getOrElse(session.subject, (0L, 0L))

      subTotals(session.subject) = (prev._1 + (session.endTime.getTime - session.startTime.getTime), prev._2 + 1)
    }

    val kv = subTotals.iterator.map(pair => (pair._1, pair._2._1.toDouble / (pair._2._2 * 3600 * 1000))).toVector.sortBy(pair => -pair._2)

    BSONDocument(
      "keys" -> BSONArray(kv.map(pair => BSONString(pair._1))),
      "values" -> BSONArray(kv.map(pair => BSONDouble(pair._2)))
    )
  }

  def subjectCumulative(sessions: Seq[Session]): BSONDocument = {

    val marks = (for (year <- 115 until 116; month <- 0 until 12) yield new Date(year, month, 1)) ++ Seq(new Date(116, 0, 1), new Date(116, 1, 1))

    val step1: Map[String, Seq[Session]] = sessions.foldLeft(Map[String, Seq[Session]]())((acc, s) =>
      acc.updated(s.subject, acc.getOrElse(s.subject, Seq[Session]()) :+ s)
    )

    val step2: Map[String, Seq[Seq[Session]]] = step1.mapValues(subSessions => groupSessions(subSessions, marks))

    val step3: Map[String, Seq[Double]] = step2.mapValues(
      vec => vec.map(sessionGroup => sessionGroup.map(sess => sess.endTime.getTime - sess.startTime.getTime).sum.toDouble / (3600 * 1000))
    )

    // We drop the first (zero) element
    val step4: Map[String, Seq[Double]] = step3.mapValues(_.scanLeft(0.0)(_ + _).drop(1))

    BSONDocument(
      "dates" -> marks.:+(new Date()),
      "values" -> BSONDocument(step4.mapValues(seq => BSONArray(seq.map(d => BSONDouble(d)))))
    )
  }


  // Split up a sessions list using a list of dates.
  def groupSessions(sessions: Seq[Session], marks: Seq[Date]): Seq[Seq[Session]] = {

    val groups = marks.foldLeft(sessions, Seq[Seq[Session]]())((acc, next) => {

      val sp = acc._1.span(_.endTime.before(next))

      val rem = sp._2.headOption.fold((None: Option[Session], None: Option[Session]))(sess => {
        if (sess.startTime.before(next)) {
          (Some(Session(sess.startTime, next, sess.subject)), Some(Session(next, sess.endTime, sess.subject)))
        } else {
          (None, Some(sess))
        }
      })

      // double counting when there is a split
      (rem._2 ++: sp._2.drop(1), acc._2 :+ (sp._1 ++ rem._1))
    })

    groups._2 :+ groups._1
  }


  def probability(sessions: Seq[Session], numBins: Int): Seq[Double] = {

    val bins = Array.fill[Int](numBins)(0)

    for (session <- sessions) {
      // Must have all sessions split on days (or use the max function)
    }

    ???
  }


  // Function that will update all (or one?) statistics
  def update(user_id: Int): Unit = {

    // 1. Get user sessions
    // 2. stats.mapValues() with sessions
    // 3. Create BSONDocument for updating user stats
    ???
  }

}
