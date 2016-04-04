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


/**
  *
  * @param mongoApi Holds the reference to the database.
  */
class Sessions(val mongoApi: ReactiveMongoApi) {

  def jsonSessionsCollection: JSONCollection = mongoApi.db.collection[JSONCollection]("sessions")

  def bsonSessionsCollection: BSONCollection = mongoApi.db.collection[BSONCollection]("sessions")


  def getStatsAsJSON(user_id: Int): Future[Option[JsObject]] = {

    val zone = ZoneId.of("America/Chicago")

    val nowMilli = ZonedDateTime.now(zone).toInstant.toEpochMilli

    // Add the current epoch millisecond to the JSON response
    def addTime(js: JsObject): JsObject = {
      js +("currentTime", JsNumber(nowMilli))
    }

    val selector = Json.obj("user_id" -> user_id)

    val projector = Json.obj("stats" -> 1, "status" -> 1, "_id" -> 0)

    jsonSessionsCollection.find(selector, projector).one[JsObject].map(_.map(js => addTime(js)))
  }


  /**
    * Starts a study session for the given user and subject.
    *
    * @param user_id The user ID for which to start a new session.
    * @param subject The subject of the new study session.
    * @return
    */
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


  /**
    * Stops the current study session and updates study stats.
    *
    * @param user_id The user ID for which to stop the current session.
    * @return
    */
  def stopSession(user_id: Int): Future[Boolean] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("_id" -> 0)

    bsonSessionsCollection.find(selector, projector).one[SessionData].flatMap { optData =>

      optData.fold(Future(false))((user: SessionData) => {

        if (!user.status.isStudying) Future(true)
        else {

          val newSession = Session(user.status.start, System.currentTimeMillis(), user.status.subject)

          // Construct the modifier
          val modifier = BSONDocument(
            "$set" -> BSONDocument(
              "stats" -> SessionStats.stats(user.sessions :+ newSession),
              "status.isStudying" -> BSONBoolean(false)
            ),
            "$push" -> BSONDocument(
              "sessions" -> newSession
            )
          )

          // Add the new session and updated stats
          bsonSessionsCollection.update(selector, modifier, multi = false).map(_.ok)
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
  def abortSession(user_id: Int): Future[Boolean] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("sessions" -> 0, "stats" -> 0, "_id" -> 0)

    bsonSessionsCollection.find(selector, projector).one[StatusData].flatMap { optUser =>

      optUser.fold(Future(false))((user: StatusData) => {

        if (!user.status.isStudying) Future(false)
        else {

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


  /**
    * Adds the given subject to the user's subject list.
    *
    * @param user_id The user ID for which to add the given subject.
    * @param subject The new subject.
    * @return
    */
  def addSubject(user_id: Int, subject: String): Future[Boolean] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("_id" -> 0)

    // Get the user's session data
    bsonSessionsCollection.find(selector, projector).one[SessionData].flatMap { optData =>

      // Check the success of the query
      optData.fold(Future(false))(data => {

        if (data.subjects.contains(subject)) Future(true)
        else {

          // Construct the modifier
          val modifier = BSONDocument(
            "$push" -> BSONDocument(
              "subjects" -> subject
            )
          )

          // Add the new subject
          bsonSessionsCollection.update(selector, modifier, multi = false).map(_.ok)
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
    bsonSessionsCollection.find(selector, projector).one[SessionData].flatMap { optData =>

      // Check the success of the query
      optData.fold(Future(false))(data => {

        // Construct the modifier
        val modifier = BSONDocument(
          "$set" -> BSONDocument(
            "stats" -> SessionStats.stats(data.sessions)
          )
        )

        // Update the stats
        bsonSessionsCollection.update(selector, modifier, multi = false).map(_.ok)
      })
    }
  }

}