package controllers

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

  def jsonTextbooksCollection: JSONCollection = db.collection[JSONCollection]("textbooks")

  def bsonTextbooksCollection: BSONCollection = db.collection[BSONCollection]("textbooks")


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

                  Ok("update successful")
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


  // Pause the current session
  // TODO: Modify the database to handle this data
  def pause() = Action.async { implicit request =>
    ???
  }


  // TODO: Send status info with stats and remove this function.
  def status(user_id: Int) = Action.async { implicit request =>

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("sessions" -> 0, "stats" -> 0, "_id" -> 0)

    bsonUsersCollection.find(selector, projector).one[User].map(opt =>
      opt.map(user =>
        if (user.status.isStudying) {
          Ok("You are studying " + user.status.subject)
        } else {
          Ok("You are not studying.")
        }
      ).getOrElse(Ok("Invalid user")))
  }


  def updateStats(user_id: Int) = Action.async { implicit request =>

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("sessions" -> 1, "_id" -> 0)

    // Query for the given user's session data
    bsonUsersCollection.find(selector, projector).one[SessionVector].flatMap { optSessionVector =>

      // Check the success of the query
      optSessionVector.fold(Future(Ok("Invalid user!")))(sessionVector => {

        // Compute the new stats
        val newStats = Stats.stats.map(p => ("stats." + p._1, p._2(sessionVector.sessions)))

        // Construct create the modifier
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
    val projector = Json.obj("stats" -> 1, "status" -> 1, "_id" -> 0)

    val futOptJson: Future[Option[JsObject]] = jsonUsersCollection.find(selector, projector).one[JsObject]

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
