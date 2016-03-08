package controllers

import forms.{SessionStart, SessionStop}
import models.{SessionVector, User, Session, Stats}

import reactivemongo.bson.BSONDocument
import scala.concurrent.{ExecutionContext, Future}

import play.api._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

import javax.inject.Inject

import reactivemongo.api.collections.bson.BSONCollection

import reactivemongo.play.json._
import play.modules.reactivemongo.json.collection._


class Application @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  def bsonUsersCollection: BSONCollection = db.collection[BSONCollection]("users")

  def bsonSessionsCollection: BSONCollection = db.collection[BSONCollection]("sessions")

  def jsonStatsCollection: JSONCollection = db.collection[JSONCollection]("stats")

  def bsonStatsCollection: BSONCollection = db.collection[BSONCollection]("stats")

  def jsonTextbooksCollection: JSONCollection = db.collection[JSONCollection]("textbooks")

  def bsonTextbooksCollection: BSONCollection = db.collection[BSONCollection]("textbooks")


  def wrapResult(optResult: Option[JsObject], failMessage: String = ""): Result = {

    val failResult = Ok(JsObject(Seq("worked" -> JsBoolean(false), "result" -> JsObject(Seq()), "message" -> JsString(failMessage))))

    optResult.fold(failResult)(result => Ok(JsObject(Seq("worked" -> JsBoolean(true), "result" -> result, "message" -> JsString("")))))
  }


  def getTextbooksByTitle(title: String) = Action.async {

    // Perform a text index search.
    val selector = Json.obj("$text" -> Json.obj(
      "$search" -> title,
      "$caseSensitive" -> false
    ))

    // Don't include the object id.
    val projector = Json.obj("_id" -> 0)

    val objList: Future[List[JsObject]] = jsonTextbooksCollection.find(selector, projector).cursor[JsObject]().collect[List]()

    objList.map(bookObjects => Ok(bookObjects.foldLeft(JsArray())((bookArr, book) => bookArr :+ book)))
  }


  // DONE: Rewrite so that we bind the form BEFORE getting the user's status
  def start() = Action.async { implicit request =>

    val futResult: Future[Result] = SessionStart.startForm.bindFromRequest()(request).fold(
      badForm => Future(BadRequest("Invalid form")),
      sessionStart => {

        val selector = BSONDocument("user_id" -> sessionStart.user_id)

        val futOptUser: Future[Option[User]] = bsonUsersCollection.find(selector).one[User]

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
                "$currentDate" -> BSONDocument(
                  "status.start" -> true
                ),
                "$set" -> BSONDocument(
                  "status.isStudying" -> true,
                  "status.subject" -> sessionStart.subject
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

        // Get basic info about the user from the database
        val futOptUser: Future[Option[User]] = bsonUsersCollection.find(selector).one[User]

        futOptUser.flatMap { optUser =>

          optUser.fold(Future(Ok("Invalid user or password")))((user: User) => {

            if (sessionStop.password != user.password) {

              Future(Ok("Invalid user or password"))

            } else if (!user.status.isStudying) {

              Future(Ok("Not Studying"))

            } else {

              // Modifier to add the new session
              val sessionModifier = BSONDocument(
                "$push" -> BSONDocument(
                  "sessions" -> Session(user.status.start, System.currentTimeMillis(), user.status.subject)
                )
              )

              val statusModifier = BSONDocument(
                "$currentDate" -> BSONDocument(
                  "status.start" -> true
                ),
                "$set" -> BSONDocument(
                  "status.isStudying" -> false,
                  "status.subject" -> ""
                )
              )

              // Update the database with the new session
              // Can these be done together in an atomic fashion?
              bsonSessionsCollection.update(selector, sessionModifier, multi = false).flatMap(sessionsUpdateResult => {

                if (sessionsUpdateResult.ok) {
                  bsonUsersCollection.update(selector, statusModifier, multi = false).flatMap(userUpdateResult => {

                    if (userUpdateResult.ok) {

                      updateStats(sessionStop.user_id)(request)
                    } else {

                      Future(Ok("Failed to update status"))
                    }

                  })
                } else {

                  Future(Ok("Failed to add session"))
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
    ???
  }


  // Pause the current session
  def pause() = Action.async { implicit request =>
    ???
  }

  def status(user_id: Int) = Action.async { implicit request =>
    ???
  }


  def updateStats(user_id: Int) = Action.async { implicit request =>

    val selector = BSONDocument("user_id" -> user_id)

    // Query for the given user's session data
    bsonSessionsCollection.find(selector).one[SessionVector].flatMap { optSessionVector =>

      // Check the success of the query
      optSessionVector.fold(Future(wrapResult(None, failMessage = "Invalid user!")))(sessionVector => {

        // Compute the new stats
        val newStats = Stats.stats.map(p => (p._1, p._2(sessionVector.sessions)))

        // Construct create the modifier
        val modifier = BSONDocument(
          "$currentDate" -> BSONDocument(
            "lastUpdate" -> true
          ),
          "$set" -> BSONDocument(
            newStats
          )
        )

        // Update the stats
        bsonStatsCollection.update(selector, modifier, multi = false).map(updateResult => {
          if (updateResult.ok) {
            Ok("update successful")
          } else {
            Ok(updateResult.message)
          }
        })
      })
    }
  }


  def getStats(user_id: Int) = Action.async {

    val selector = Json.obj("user_id" -> user_id)

    // Don't send the object id
    val projector = Json.obj("_id" -> 0, "user_id" -> 0)

    val futOptJson: Future[Option[JsObject]] = jsonStatsCollection.find(selector, projector).one[JsObject]

    val futResult: Future[Result] = futOptJson.map(opt => Ok(opt.getOrElse(JsObject(Seq()))))

    futResult
  }

  def index = Action {
    Ok(views.html.index())
  }

  def about = Action {
    Ok(views.html.about())
  }

}
