package models

import reactivemongo.bson._

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
      total + (session.endInstant - session.startInstant)
    }).toDouble / 3600)
  }


  // For the applicable stats, have such a function
  def updatedTotal(oldTotal: BSONDouble, newSession: Session): BSONDouble = {
    BSONDouble(oldTotal.value + (newSession.endInstant - newSession.startInstant).toDouble / 3600)
  }


  def subjectTotals(sessions: Seq[Session]): BSONDocument = {

    val kv = sessions.foldLeft(Map[String, Long]())((totals, session) => {

      val previous = totals.getOrElse(session.subject, 0L)

      totals.updated(session.subject, previous + (session.endInstant - session.startInstant))
    }).toVector.sortBy(pair => -pair._2)

    BSONDocument(
      "keys" -> BSONArray(kv.map(pair => BSONString(pair._1))),
      "values" -> BSONArray(kv.map(pair => BSONDouble(pair._2.toDouble / 3600)))
    )
  }


  def cumulative(sessions: Seq[Session]): BSONDocument = {

    val cum = sessions.foldLeft((Seq[(Long, java.util.Date)](), 0L)) { (acc, next: Session) =>

      val sum: Long = next.endInstant - next.startInstant + acc._2

      (acc._1 :+(sum, next.endTime), sum)
    }._1

    BSONDocument(
      "dates" -> cum.map(pair => pair._2),
      "values" -> cum.map(pair => BSONDouble(pair._1.toDouble / 3600))
    )
  }

  def averageSession(sessions: Seq[Session]): BSONDocument = {

    val subTotals = mutable.Map[String, (Long, Long)]()

    for (session <- sessions) {

      val prev = subTotals.getOrElse(session.subject, (0L, 0L))

      subTotals(session.subject) = (prev._1 + (session.endInstant - session.startInstant), prev._2 + 1)
    }

    val kv = subTotals.iterator.map(pair => (pair._1, pair._2._1.toDouble / (pair._2._2 * 3600))).toVector.sortBy(pair => -pair._2)

    BSONDocument(
      "keys" -> BSONArray(kv.map(pair => BSONString(pair._1))),
      "values" -> BSONArray(kv.map(pair => BSONDouble(pair._2)))
    )
  }

  def subjectCumulative(sessions: Seq[Session]): BSONDocument = {

    val cums = sessions.foldLeft(Map[String, Vector[Long]]())((acc, s) =>
      acc.updated(s.subject, acc.getOrElse(s.subject, Vector[Long]()) :+ (s.endInstant - s.startInstant))
    ).mapValues(vec => vec.foldLeft((Vector[Double](), 0L))(
      (acc, next) => (acc._1 :+ ((acc._2 + next).toDouble / 3600), acc._2 + next))
    )

    BSONDocument(cums.mapValues(pair => BSONArray(pair._1.map(d => BSONDouble(d)))))
  }


  // Function that will update all (or one?) statistics
  def update(user_id: Int): Unit = {

    // 1. Get user sessions
    // 2. stats.mapValues() with sessions
    // 3. Create BSONDocument for updating user stats
    ???
  }

}
