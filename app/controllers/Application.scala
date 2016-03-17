package controllers

import java.time.{ZoneId, ZonedDateTime}

import forms.{SessionStart, SessionStop}
import models.{Session, UserWithSessions, UserStatus, User, SessionVector, Stats}

import reactivemongo.bson.{BSONBoolean, BSONDocument}
import scala.concurrent.{ExecutionContext, Future}

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

import javax.inject.Inject

import reactivemongo.api.collections.bson.BSONCollection

import reactivemongo.play.json._
import play.modules.reactivemongo.json.collection._


class Application @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  def jsonUsersCollection: JSONCollection = db.collection[JSONCollection]("users")

  def bsonUsersCollection: BSONCollection = db.collection[BSONCollection]("users")


  // DONE: Rewrite so that we bind the form BEFORE getting the user's status
  def start() = Action.async { implicit request =>

    val futResult: Future[Result] = SessionStart.startForm.bindFromRequest()(request).fold(
      badForm => Future(BadRequest("Invalid form")),
      sessionStart => {

        val selector = BSONDocument("user_id" -> sessionStart.user_id)

        val projector = BSONDocument("sessions" -> 0, "_id" -> 0)

        val futOptUser: Future[Option[User]] = bsonUsersCollection.find(selector, projector).one[User]

        futOptUser.flatMap { optUser =>

          optUser.fold(Future(BadRequest("Invalid user or password")))((user: User) => {

            if (sessionStart.password != user.password) {

              Future(Ok("Invalid user or password"))
            } else if (user.status.isStudying) {

              Future(BadRequest("Already Studying"))
            } else if (!user.subjects.contains(sessionStart.subject)) {

              Future(BadRequest("Invalid Subject"))
            } else {

              val modifier = BSONDocument(
                "$set" -> BSONDocument(
                  "status.isStudying" -> true,
                  "status.subject" -> sessionStart.subject,
                  "status.start" -> System.currentTimeMillis()
                )
              )

              // Update the status
              bsonUsersCollection.update(selector, modifier, multi = false).map(updateResult => {
                if (updateResult.ok) {
                  Ok("now studying " + sessionStart.subject)
                } else {
                  Ok(updateResult.getMessage)
                }
              })
            }
          })
        }
      }
    )

    futResult
  }


  def stop() = Action.async { implicit request =>

    val futResult: Future[Result] = SessionStop.stopForm.bindFromRequest()(request).fold(

      // When we can't bind the form
      badForm => Future(Ok("Invalid Form")),

      // When we can bind the form
      sessionStop => {

        // We want to pull documents for the given user
        val selector = BSONDocument("user_id" -> sessionStop.user_id)

        val projector = BSONDocument("_id" -> 0)

        // Get basic info about the user from the database
        val futOptUser: Future[Option[UserWithSessions]] = bsonUsersCollection.find(selector, projector).one[UserWithSessions]

        futOptUser.flatMap { optUser =>

          optUser.fold(Future(Ok("Invalid user or password")))((user: UserWithSessions) => {

            if (sessionStop.password != user.password) {

              Future(Ok("Invalid user or password"))

            } else if (!user.status.isStudying) {

              Future(Ok("Not Studying"))

            } else {

              val newSession = Session(user.status.start, System.currentTimeMillis(), user.status.subject)

              // Compute the new stats
              val newStats = Stats.stats.map(p => ("stats." + p._1, p._2(user.sessions :+ newSession)))

              // Construct the modifier
              val modifier = BSONDocument(
                "$currentDate" -> BSONDocument(
                  "stats.lastUpdate" -> true
                ),
                "$set" -> BSONDocument(
                  newStats.toSeq ++ Seq("status.isStudying" -> BSONBoolean(false))
                ),
                "$push" -> BSONDocument(
                  "sessions" -> newSession
                )
              )

              // Update the database with the new session
              bsonUsersCollection.update(selector, modifier, multi = false).map(updateResult => {

                if (updateResult.ok) {

                  Ok("updated")
                } else {

                  Ok("failed to update")
                }

              })
            }
          })
        }
      }
    )

    // Return our future result
    futResult
  }


  // Abort the current session
  def abort() = Action.async { implicit request =>

    val futResult: Future[Result] = SessionStop.stopForm.bindFromRequest()(request).fold(
      badForm => Future(Ok("Invalid form")),
      sessionStop => {

        val selector = BSONDocument("user_id" -> sessionStop.user_id)

        val projector = BSONDocument("sessions" -> 0, "stats" -> 0, "_id" -> 0)

        val futOptUser: Future[Option[User]] = bsonUsersCollection.find(selector, projector).one[User]

        futOptUser.flatMap { optUser =>

          optUser.fold(Future(Ok("Invalid user or password")))((user: User) => {

            if (sessionStop.password != user.password) {

              Future(Ok("Invalid user or password"))
            } else if (!user.status.isStudying) {

              Future(Ok("Not Studying"))
            } else {

              val modifier = BSONDocument(
                "$set" -> BSONDocument(
                  "status.isStudying" -> false
                )
              )

              // Update the status
              bsonUsersCollection.update(selector, modifier, multi = false).map(updateResult => {
                if (updateResult.ok) {
                  Ok("aborted studying " + user.status.subject)
                } else {
                  Ok(updateResult.getMessage)
                }
              })
            }
          })
        }
      }
    )

    futResult
  }


  def updateStats() = Action.async { implicit request =>

    SessionStop.stopForm.bindFromRequest().fold(badForm => Future(Ok("Bad Form")), goodForm => {

      val selector = BSONDocument("user_id" -> goodForm.user_id)

      val projector = BSONDocument("_id" -> 0)

      // Query for the given user's session data
      bsonUsersCollection.find(selector, projector).one[UserWithSessions].flatMap { optUser =>

        // Check the success of the query
        optUser.fold(Future(Ok("Invalid user or password.")))(user => {

          if (user.password != goodForm.password) {
            Future(Ok("Invalid user or password."))
          } else {

            // Compute the new stats
            val newStats = Stats.stats.map(p => ("stats." + p._1, p._2(user.sessions)))

            // Construct the modifier
            val modifier = BSONDocument(
              "$currentDate" -> BSONDocument(
                "stats.lastUpdate" -> true
              ),
              "$set" -> BSONDocument(
                newStats
              )
            )

            // Update the stats
            bsonUsersCollection.update(selector, modifier, multi = false).map(updateResult => {
              if (updateResult.ok) {
                Ok("updated")
              } else {
                Ok(updateResult.message)
              }
            })
          }
        })
      }

    })
  }


  def getStats(user_id: Int) = Action.async {

    val nowMilli = ZonedDateTime.now(ZoneId.of("America/Chicago")).toInstant.toEpochMilli

    // Add the current epoch millisecond to the JSON response
    def addTime(js: JsObject): JsObject = {
      js +("currentTime", JsNumber(nowMilli))
    }

    val selector = Json.obj("user_id" -> user_id)

    val projector = Json.obj("stats" -> 1, "status" -> 1, "_id" -> 0)

    val futOptJson: Future[Option[JsObject]] = jsonUsersCollection.find(selector, projector).one[JsObject]

    // Return the result with the current time in the users timezone
    val futResult: Future[Result] = futOptJson.map(optJson => Ok(addTime(optJson.getOrElse(JsObject(Seq())))))

    futResult
  }

  def index = Action {
    Ok(views.html.home())
  }

  def about = Action {
    Ok(views.html.about())
  }

}
