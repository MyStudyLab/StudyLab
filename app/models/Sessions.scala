package models

import java.time
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalTime, ZoneId, ZonedDateTime}

import helpers.ResultInfo
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONArray, BSONDocument, BSONDouble, BSONInteger, BSONLong, BSONString}
import reactivemongo.play.json._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.mutable


/**
  * Model layer to manage study sessions.
  *
  * @param mongoApi Holds the reference to the database.
  */
class Sessions(val mongoApi: ReactiveMongoApi) {

  // An interface to the sessions collection as JSON
  def jsonSessionsCollection: JSONCollection = mongoApi.db.collection[JSONCollection]("sessions")

  // An interface to the sessions collection as BSON
  def bsonSessionsCollection: BSONCollection = mongoApi.db.collection[BSONCollection]("sessions")

  // A result indicating that the user was already studying.
  val alreadyStudying = ResultInfo(success = false, "Already studying")

  // A result indicating that the user was not studying.
  val notStudying = ResultInfo(success = false, "Not studying")

  // A result indicating that the given subject was invalid.
  val invalidSubject = ResultInfo(success = false, "Invalid subject")


  /**
    * Get study stats as JSON.
    *
    * @param user_id The user ID for which to get study stats.
    * @return
    */
  def getStatsAsJSON(user_id: Int): Future[JsObject] = {

    // Add the current epoch millisecond to the JSON response
    def addTime(js: JsObject): JsObject = {
      js + ("currentTime" -> JsNumber(System.currentTimeMillis()))
    }

    // Add the success of the request to the JSON response.
    def addResponseStatus(js: JsObject, success: Boolean): JsObject = {
      js + ("success" -> JsBoolean(success))
    }

    val selector = Json.obj("user_id" -> user_id)

    val projector = Json.obj("stats" -> 1, "status" -> 1, "subjects" -> 1, "_id" -> 0)

    jsonSessionsCollection.find(selector, projector).one[JsObject].map(optJS =>
      optJS.fold(Json.obj("success" -> false))(js => addResponseStatus(addTime(js), success = true))
    )
  }


  /**
    * Starts a study session for the given user and subject.
    *
    * @param user_id The user ID for which to start a new session.
    * @param subject The subject of the new study session.
    * @return
    */
  def startSession(user_id: Int, subject: String): Future[ResultInfo] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("status" -> 1, "subjects" -> 1, "user_id" -> 1, "_id" -> 0)

    bsonSessionsCollection.find(selector, projector).one[StatusAndSubjects].flatMap(opt =>

      opt.fold(Future(ResultInfo.badUsernameOrPass))(statsAndSubs => {

        if (statsAndSubs.status.isStudying) Future(alreadyStudying)
        else if (!statsAndSubs.subjects.map(_.name).contains(subject)) Future(invalidSubject)
        else {

          // The modifier needed to start a session
          val modifier = BSONDocument(
            "$set" -> BSONDocument(
              "status.isStudying" -> true,
              "status.subject" -> subject,
              "status.start" -> System.currentTimeMillis()
            )
          )

          // Update the status
          bsonSessionsCollection.update(selector, modifier, multi = false).map(result =>
            if (result.ok) ResultInfo(success = true, s"Now studying $subject")
            else ResultInfo.databaseError
          )
        }
      })
    )
  }


  /**
    * Stops the current study session and updates study stats.
    *
    * @param user_id The user ID for which to stop the current session.
    * @return
    */
  def stopSession(user_id: Int): Future[ResultInfo] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("_id" -> 0)

    bsonSessionsCollection.find(selector, projector).one[SessionData].flatMap { opt =>

      opt.fold(Future(ResultInfo.badUsernameOrPass))(sessionData => {

        if (!sessionData.status.isStudying) Future(notStudying)
        else {

          val newSession = Session(sessionData.status.subject, sessionData.status.start, System.currentTimeMillis())

          // The modifier needed to stop a session
          val modifier = BSONDocument(
            "$set" -> BSONDocument(
              "stats" -> Sessions.stats(sessionData.sessions :+ newSession),
              "status.isStudying" -> false
            ),
            "$push" -> BSONDocument(
              "sessions" -> newSession
            )
          )

          // Add the new session and updated stats
          bsonSessionsCollection.update(selector, modifier, multi = false).map(result =>
            if (result.ok) ResultInfo(success = true, "Finished studying")
            else ResultInfo.databaseError)
        }
      })
    }
  }


  /**
    * Aborts the current study session.
    *
    * @param user_id The user ID for which to abort the current session.
    * @return
    */
  def abortSession(user_id: Int): Future[ResultInfo] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("status" -> 1, "subjects" -> 1, "_id" -> 0)

    bsonSessionsCollection.find(selector, projector).one[StatusAndSubjects].flatMap { opt =>

      opt.fold(Future(ResultInfo.badUsernameOrPass))(statsAndSubs => {

        if (!statsAndSubs.status.isStudying) Future(notStudying)
        else {

          // The modifier needed to abort a session
          val modifier = BSONDocument(
            "$set" -> BSONDocument(
              "status.isStudying" -> false
            )
          )

          // Update the status
          bsonSessionsCollection.update(selector, modifier, multi = false).map(result =>
            if (result.ok) ResultInfo(success = true, "Study session aborted")
            else ResultInfo.databaseError)
        }
      })
    }
  }


  /**
    * Adds the given subject to the user's subject list.
    *
    * @param user_id The user ID for which to add the given subject.
    * @param subject The new subject.
    * @return
    */
  def addSubject(user_id: Int, subject: String): Future[ResultInfo] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("status" -> 1, "subjects" -> 1, "_id" -> 0)

    // Get the user's session data
    bsonSessionsCollection.find(selector, projector).one[StatusAndSubjects].flatMap { opt =>

      // Check the success of the query
      opt.fold(Future(ResultInfo.badUsernameOrPass))(statsAndSubs => {

        if (statsAndSubs.subjects.map(_.name).contains(subject)) Future(ResultInfo(success = true, "Subject already added."))
        else {


          // The modifier needed to add a subject
          val modifier = BSONDocument(
            "$push" -> BSONDocument(
              "subjects" -> Subject(subject, System.currentTimeMillis(), isLanguage = false, "")
            )
          )

          // Add the new subject
          bsonSessionsCollection.update(selector, modifier, multi = false).map(result =>
            if (result.ok) ResultInfo(success = true, s"Successfully added $subject.")
            else ResultInfo.databaseError)
        }
      })
    }
  }


  /**
    * Removes the given subject from the user's subject list.
    *
    * @param user_id The user ID for which to remove the subject.
    * @param subject The subject to remove.
    * @return
    */
  def removeSubject(user_id: Int, subject: String): Future[ResultInfo] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("sessions" -> 1, "status" -> 1, "subjects" -> 1, "_id" -> 0)

    // Get the user's session data
    bsonSessionsCollection.find(selector, projector).one[SessionData].flatMap { opt =>

      // Check the success of the query
      opt.fold(Future(ResultInfo.badUsernameOrPass))(sessionData => {

        if (!sessionData.subjects.map(_.name).contains(subject)) {
          Future(ResultInfo(success = false, "Invalid subject."))
        } else if (sessionData.sessions.map(_.subject).toSet.contains(subject)) {
          Future(ResultInfo(success = false, s"Can't remove $subject. It has been studied."))
        } else {

          // New subject vector without the subject.
          val newSubjects = sessionData.subjects.filterNot(_.name == subject)

          // The modifier needed to add a subject
          val modifier = BSONDocument(
            "$set" -> BSONDocument(
              "subjects" -> newSubjects
            )
          )

          // Remove the subject
          bsonSessionsCollection.update(selector, modifier, multi = false).map(result =>
            if (result.ok) ResultInfo(success = true, s"Successfully removed $subject.")
            else ResultInfo.databaseError)
        }
      })
    }
  }


  /**
    * Rename an existing subject.
    *
    * @param user_id The user ID for which to rename the given subject.
    * @param oldName The subject to rename.
    * @param newName The new name.
    * @return
    */
  def renameSubject(user_id: Int, oldName: String, newName: String): Future[ResultInfo] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("stats" -> 0, "_id" -> 0)

    // Get the user's session data
    bsonSessionsCollection.find(selector, projector).one[SessionData].flatMap { opt =>

      // Check the success of the query
      opt.fold(Future(ResultInfo.badUsernameOrPass))(sessionData => {

        sessionData.subjects.find(_.name == oldName).fold(
          Future(ResultInfo(success = false, s"$oldName is an invalid subject."))
        )(oldSub => {

          // Check if the new subject name is already in use
          if (sessionData.subjects.map(_.name).contains(newName)) {
            Future(ResultInfo(success = false, s"Can't rename to $newName. It is an existing subject."))
          } else {

            val newSub = Subject(newName, oldSub.added, oldSub.isLanguage, oldSub.description)

            // Updated subject list using the new subject name
            val newSubjects = sessionData.subjects.filterNot(_.name == oldName) :+ newSub

            // Updated session data using the new subject name
            val newSessions = sessionData.sessions.map(session => {
              if (session.subject == oldName) Session(newName, session.startTime, session.endTime)
              else session
            })

            // Updated stats using the new subject name
            val newStats = Sessions.stats(newSessions)

            // The modifier needed to rename a subject
            val modifier = BSONDocument(
              "$set" -> BSONDocument(
                "subjects" -> newSubjects,
                "sessions" -> newSessions,
                "stats" -> newStats
              )
            )

            // Add the new subject
            bsonSessionsCollection.update(selector, modifier, multi = false).map(result =>
              if (result.ok) ResultInfo(success = true, s"Successfully renamed $oldName to $newName.")
              else ResultInfo.databaseError)
          }
        })
      })
    }
  }


  /**
    * Merge two subjects, combining their sessions.
    *
    * TODO: Needs testing
    *
    * @param user_id   The user ID for which to merge the subjects.
    * @param absorbed  The subject that will be absorbed.
    * @param absorbing The subject that will absorb the other.
    * @return
    */
  def mergeSubjects(user_id: Int, absorbed: String, absorbing: String): Future[ResultInfo] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("status" -> 1, "subjects" -> 1, "sessions" -> 1, "_id" -> 0)

    // Get the user's session data
    bsonSessionsCollection.find(selector, projector).one[SessionData].flatMap { opt =>

      // Check the success of the query
      opt.fold(Future(ResultInfo.badUsernameOrPass))(data => {

        val subjectNames = data.subjects.map(_.name)

        if (!subjectNames.contains(absorbed)) {
          Future(ResultInfo(success = true, s"Can't merge. $absorbed is an invalid subject."))
        } else if (!subjectNames.contains(absorbing)) {
          Future(ResultInfo(success = true, s"Can't merge. $absorbing is an invalid subject."))
        } else {

          // Updated subject vector without the absorbed subject name
          val newSubjects = data.subjects.filterNot(_.name == absorbed)

          // Updated session vector without the absorbed subject name
          val newSessions = data.sessions.map(session => {
            if (session.subject == absorbed) Session(absorbing, session.startTime, session.endTime)
            else session
          })

          // Updated stats document computed from the new session vector
          val newStats = Sessions.stats(newSessions)

          // The modifier needed to merge the subjects
          val modifier = BSONDocument(
            "$set" -> BSONDocument(
              "subjects" -> newSubjects,
              "sessions" -> newSessions,
              "stats" -> newStats
            )
          )

          // Merge the subjects
          bsonSessionsCollection.update(selector, modifier, multi = false).map(result =>
            if (result.ok) ResultInfo(success = true, s"Successfully merged $absorbed into $absorbing.")
            else ResultInfo.databaseError)
        }
      })
    }
  }


  /**
    * Updates the study stats.
    *
    * @param user_id The user ID for which to update the study stats.
    * @return
    */
  def updateStats(user_id: Int): Future[Boolean] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("_id" -> 0)

    // Query for the given user's session data
    bsonSessionsCollection.find(selector, projector).one[SessionData].flatMap { opt =>

      // Check the success of the query
      opt.fold(Future(false))(sessionData => {

        // The modifier needed to update the study stats
        val modifier = BSONDocument(
          "$set" -> BSONDocument(
            "stats" -> Sessions.stats(sessionData.sessions)
          )
        )

        // Update the stats
        bsonSessionsCollection.update(selector, modifier, multi = false).map(_.ok)
      })
    }
  }

}


object Sessions {


  def stats(sessions: Vector[Session]): BSONDocument = {

    val zone = ZoneId.of("America/Chicago")

    val totalHours = total(sessions)

    val dailyAverage = totalHours / daysSinceStart(zone)(sessions)

    val streaks = currentAndLongestStreaks(sessions)

    BSONDocument(
      "introMessage" -> introMessage(sessions),
      "total" -> BSONDouble(totalHours),
      "dailyAverage" -> BSONDouble(dailyAverage),
      "currentStreak" -> BSONInteger(streaks._1),
      "longestStreak" -> BSONInteger(streaks._2),
      "subjectTotals" -> subjectTotals(sessions).toVector.sortBy(p => -p._2).map(p => BSONArray(p._1, p._2)),
      "cumulative" -> cumulative(sessions).map(p => BSONArray(BSONLong(p._1), BSONDouble(p._2))),
      "averageSession" -> averageSession(sessions).toVector.sortBy(p => -p._2).map(p => BSONArray(p._1, p._2)),
      "subjectCumulative" -> subjectCumulativeGoogle(sessions),
      "probability" -> probability(100)(sessions).map(p => BSONArray(BSONArray(p._1.getHour, p._1.getMinute, p._1.getSecond), BSONDouble(p._2))),
      "todaysSessions" -> todaysSessionsGoogle(sessions),
      "slidingAverage" -> slidingAverage(15)(sessions).map(p => BSONArray(p._1, p._2)),
      "lastUpdated" -> System.currentTimeMillis()
    )
  }


  /**
    * Total duration of the sessions.
    *
    * @param sessions The vector of sessions to sum.
    * @return
    */
  def total(sessions: Vector[Session]): Double = {
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
  def average(sessions: Vector[Session]): Double = {
    sessions.foldLeft(0L)((total: Long, session: Session) => {
      total + session.durationMillis()
    }).toDouble / (3600 * 1000 * sessions.length)
  }


  def introMessage(sessions: Vector[Session]): BSONDocument = {

    val zone = ZoneId.of("America/Chicago")

    val startZDT = startDate(zone)(sessions)

    val todaysSessionsVec = todaysSessions(zone)(sessions)

    val todaysTotal = total(todaysSessionsVec)

    BSONDocument(
      "start" -> BSONArray(BSONInteger(startZDT.getMonthValue), BSONInteger(startZDT.getDayOfMonth), BSONInteger(startZDT.getYear)),
      "daysSinceStart" -> BSONInteger(daysSinceStart(zone)(sessions).toInt),
      "todaysTotal" -> todaysTotal,
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


  def subjectTotals(sessions: Vector[Session]): Map[String, Double] = {

    val kv = sessions.foldLeft(Map[String, Long]())((totals, session) => {

      val previous = totals.getOrElse(session.subject, 0L)

      totals.updated(session.subject, previous + session.durationMillis())
    }).mapValues(total => total.toDouble / (3600 * 1000))

    kv
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


  def slidingAverage(radius: Int)(sessions: Vector[Session]): Vector[(Long, Double)] = {

    val dailyTotals = groupDays(ZoneId.of("America/Chicago"))(sessions).map(p => (p._1._1, total(p._2)))

    val startIndex = radius
    val endIndex = dailyTotals.length - radius
    val windowSize = 1 + 2 * radius

    for (i <- Vector.range(startIndex, endIndex)) yield {
      (dailyTotals(i)._1, dailyTotals.slice(i - radius, i + radius).map(_._2).sum / windowSize)
    }
  }


  def averageSession(sessions: Seq[Session]): Map[String, Double] = {

    val subTotals = mutable.Map[String, (Long, Long)]()

    for (session <- sessions) {

      val prev = subTotals.getOrElse(session.subject, (0L, 0L))

      subTotals(session.subject) = (prev._1 + session.durationMillis(), prev._2 + 1)
    }

    subTotals.mapValues(pair => pair._1.toDouble / (pair._2 * 3600 * 1000)).toMap
  }


  def subjectCumulative(sessions: Vector[Session]): (Vector[Long], Map[String, Vector[Double]]) = {

    val marks = monthMarksSince(ZoneId.of("America/Chicago"))(sessions.head.startTime)

    val step1: Map[String, Vector[Session]] = sessions.groupBy(_.subject)

    val step2: Map[String, Vector[Vector[Session]]] = step1.mapValues(subSessions => groupSessions(subSessions, marks))

    val step3: Map[String, Vector[Double]] = step2.mapValues(groups => groups.map(sessionGroup => total(sessionGroup)))

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
          (Some(Session(sess.subject, sess.startTime, next)), Some(Session(sess.subject, next, sess.endTime)))
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


  def dailyTotalCounts(zone: ZoneId)(sessions: Vector[Session]): Vector[(Int, Int)] = {

    val days = dailyTotals(zone)(sessions)

    days.groupBy(dailyTotal => math.ceil(dailyTotal).toInt).mapValues(_.length).toVector
  }


  def dailyTotalCountsGoogle(sessions: Vector[Session]): BSONDocument = {

    val zone = ZoneId.of("America/Chicago")

    val dailyTotalGroups = dailyTotalCounts(zone)(sessions)

    BSONDocument(
      "columns" -> BSONArray(
        BSONArray(BSONString("number"), BSONString("Duration")),
        BSONArray(BSONString("number"), BSONString("Sessions Less Than"))
      ),
      "rows" -> BSONArray(dailyTotalGroups.map(p => BSONArray(p._1, p._2)))
    )

  }


  def probabilityOfDailyTotal(zone: ZoneId)(sessions: Vector[Session]): Vector[(Int, Int)] = {

    dailyTotals(zone)(sessions).map(dailyTotal => (100 * dailyTotal / 24).toInt).groupBy(a => a).mapValues(_.length).toVector

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
    val first = unsplitSessions.lastOption.map(s => Session(s.subject, math.max(s.startTime, since), s.endTime))

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
      "rows" -> BSONArray(todaysSessions(ZoneId.of("America/Chicago"))(sessions).map(s => BSONArray(BSONString("What I've Done Today"), BSONString(s.subject), BSONLong(s.startTime), BSONLong(s.endTime))))
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

}
