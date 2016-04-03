package models

import java.time.{ZoneId, ZonedDateTime}

import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONBoolean, BSONDocument}
import reactivemongo.play.json._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext


class Sessions(val api: ReactiveMongoApi) {

  def jsonSessionsCollection: JSONCollection = api.db.collection[JSONCollection]("sessions")

  def bsonSessionsCollection: BSONCollection = api.db.collection[BSONCollection]("sessions")


  def getStatsAsJSON(user_id: Int): Future[Option[JsObject]] = {

    val zone = ZoneId.of("America/Chicago")

    val nowMilli = ZonedDateTime.now(zone).toInstant.toEpochMilli

    // Add the current epoch millisecond to the JSON response
    def addTime(js: JsObject): JsObject = {
      js +("currentTime", JsNumber(nowMilli))
    }

    // Get start of day in millis and compare to last updated
    val startofDayMilli = SessionStats.startOfDay(zone)(nowMilli)

    val selector = Json.obj("user_id" -> user_id)

    val projector = Json.obj("stats" -> 1, "status" -> 1, "_id" -> 0)

    val futOptJson = jsonSessionsCollection.find(selector, projector).one[JsObject]

    futOptJson.map(_.map(js => {

      if ((js \ "stats" \ "lastUpdated").as[Long] < startofDayMilli) {
        updateStats(user_id)
      }

      addTime(js)
    }))
  }


  def startSession(user_id: Int, subject: String): Future[Boolean] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("sessions" -> 0, "_id" -> 0)

    val futOptUser = bsonSessionsCollection.find(selector, projector).one[StatusData]

    futOptUser.flatMap { optUser =>

      optUser.fold(Future(false))((user: StatusData) => {

        if (user.status.isStudying || !user.subjects.contains(subject)) {

          Future(false)
        } else {

          val modifier = BSONDocument(
            "$set" -> BSONDocument(
              "status.isStudying" -> true,
              "status.subject" -> subject,
              "status.start" -> System.currentTimeMillis()
            )
          )

          // Update the status
          bsonSessionsCollection.update(selector, modifier, multi = false).map(_.ok)
        }
      })
    }
  }


  def stopSession(user_id: Int): Future[Boolean] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("_id" -> 0)

    val futOptUser = bsonSessionsCollection.find(selector, projector).one[SessionData]

    futOptUser.flatMap { optUser =>

      optUser.fold(Future(false))((user: SessionData) => {

        if (!user.status.isStudying) {

          Future(false)

        } else {

          val newSession = Session(user.status.start, System.currentTimeMillis(), user.status.subject)

          // Construct the modifier
          val modifier = BSONDocument(
            "$set" -> BSONDocument(
              "stats" -> SessionStats.stats(SessionVector(user.sessions :+ newSession)),
              "status.isStudying" -> BSONBoolean(false)
            ),
            "$push" -> BSONDocument(
              "sessions" -> newSession
            )
          )

          // Update the database with the new session and stats
          bsonSessionsCollection.update(selector, modifier, multi = false).map(_.ok)
        }
      })
    }
  }


  // Abort the current session
  def abortSession(user_id: Int): Future[Boolean] = {


    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("sessions" -> 0, "stats" -> 0, "_id" -> 0)

    val futOptUser = bsonSessionsCollection.find(selector, projector).one[StatusData]

    futOptUser.flatMap { optUser =>

      optUser.fold(Future(false))((user: StatusData) => {

        if (!user.status.isStudying) {

          Future(false)
        } else {

          val modifier = BSONDocument(
            "$set" -> BSONDocument(
              "status.isStudying" -> false
            )
          )

          // Update the status
          bsonSessionsCollection.update(selector, modifier, multi = false).map(_.ok)
        }
      })
    }
  }


  def addSubject(user_id: Int, subject: String): Future[Boolean] = {
    ???
  }


  def updateStats(user_id: Int): Future[Boolean] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("_id" -> 0)

    // Query for the given user's session data
    bsonSessionsCollection.find(selector, projector).one[SessionData].flatMap { optUser =>

      // Check the success of the query
      optUser.fold(Future(false))(user => {

        // Construct the modifier
        val modifier = BSONDocument(
          "$set" -> BSONDocument(
            "stats" -> Stats.stats(SessionVector(user.sessions))
          )
        )

        // Update the stats
        bsonSessionsCollection.update(selector, modifier, multi = false).map(_.ok)
      })
    }
  }

}