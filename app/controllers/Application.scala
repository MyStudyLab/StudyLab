package controllers

import java.time.{ZoneId, ZonedDateTime}

import forms.{SessionStart, SessionStop, UpdateForm}
import models.{Session, _}
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

  def jsonQuotesCollection: JSONCollection = db.collection[JSONCollection]("quotes")

  def bsonBooksCollection: BSONCollection = db.collection[BSONCollection]("books")

  def jsonBooksCollection: JSONCollection = db.collection[JSONCollection]("books")

  def bsonMoviesCollection: BSONCollection = db.collection[BSONCollection]("movies")

  def jsonMoviesCollection: JSONCollection = db.collection[JSONCollection]("movies")


  def start() = Action.async { implicit request =>

    val futResult: Future[Result] = SessionStart.startForm.bindFromRequest()(request).fold(
      badForm => Future(Ok("Invalid form")),
      sessionStart => {

        val selector = BSONDocument("user_id" -> sessionStart.user_id)

        val projector = BSONDocument("sessions" -> 0, "_id" -> 0)

        val futOptUser: Future[Option[User]] = bsonUsersCollection.find(selector, projector).one[User]

        futOptUser.flatMap { optUser =>

          optUser.fold(Future(Ok("Invalid user or password")))((user: User) => {

            if (sessionStart.password != user.password) {

              Future(Ok("Invalid user or password"))
            } else if (user.status.isStudying) {

              Future(Ok("Already Studying"))
            } else if (!user.subjects.contains(sessionStart.subject)) {

              Future(Ok("Invalid Subject"))
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

            // Construct the modifier
            val modifier = BSONDocument(
              "$set" -> BSONDocument(
                "stats" -> Stats.stats(SessionVector(user.sessions))
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


  def updateBooks(user_id: Int) = Action.async { implicit request =>

    // Will look for the user with the given id
    val selector = BSONDocument("user_id" -> user_id)

    // Will get book data here
    val projector = BSONDocument("_id" -> 0, "books" -> 1)

    // Get books for user
    bsonBooksCollection.find(selector, projector).one[BookVector].flatMap { optBookVector =>

      optBookVector.fold(Future(Ok("Failed to get books")))(bookVec => {

        // Construct the modifier
        val modifier = BSONDocument(
          "$set" -> BSONDocument(
            "stats" -> Stats.stats(bookVec)
          )
        )

        // Update the book stats
        bsonBooksCollection.update(selector, modifier, multi = false).map(updateResult => {
          if (updateResult.ok) {
            Ok("updated books")
          } else {
            Ok(updateResult.message)
          }
        })
      })
    }
  }

  // Handles all requests that update the database
  def routeUpdate(collection: String) = Action.async { implicit request =>

    // Check the collection here, before making any requests to db

    UpdateForm.form.bindFromRequest().fold(badForm => Future(Ok("Bad Form")), goodForm => {

      val selector = BSONDocument("user_id" -> goodForm.user_id)

      val projector = BSONDocument("_id" -> 0, "sessions" -> 0, "stats" -> 0)

      // Check user credentials
      bsonUsersCollection.find(selector, projector).one[User].flatMap { optUser =>

        optUser.fold(Future(Ok("Invalid user or password")))(user => {

          if (goodForm.password != user.password) {
            Future(Ok("Invalid username or password"))
          } else {

            if (collection == "movies") {
              updateMovies(goodForm.user_id)(request)
            } else if (collection == "books") {
              updateBooks(goodForm.user_id)(request)
            } else {
              Future(Ok("Invalid collection"))
            }
          }
        })

      }
    })
  }

  def updateMovies(user_id: Int) = Action.async { implicit request =>

    // Will look for the user with the given id
    val selector = BSONDocument("user_id" -> user_id)

    // Will get book data here
    val projector = BSONDocument("_id" -> 0, "movies" -> 1)

    // Get books for user
    bsonMoviesCollection.find(selector, projector).one[MovieVector].flatMap { optMovieVector =>

      optMovieVector.fold(Future(Ok("Failed to get movies")))(movieVec => {

        // Construct the modifier
        val modifier = BSONDocument(
          "$set" -> BSONDocument(
            "stats" -> Stats.stats(movieVec)
          )
        )

        // Update the movie stats
        bsonMoviesCollection.update(selector, modifier, multi = false).map(updateResult => {
          if (updateResult.ok) {
            Ok("updated movies")
          } else {
            Ok(updateResult.message)
          }
        })
      })
    }
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

  def getQuotes(user_id: Int) = Action.async {

    val selector = Json.obj("user_id" -> 1)

    val projector = Json.obj("_id" -> 0, "user_id" -> 0)

    val futOptJson = jsonQuotesCollection.find(selector, projector).one[JsObject]

    futOptJson.map(optJson => Ok(optJson.getOrElse(JsObject(Seq())).value("quotes")))
  }

  def getBooks(user_id: Int) = Action.async {

    val selector = Json.obj("user_id" -> 1)

    val projector = Json.obj("_id" -> 0, "user_id" -> 0)

    val futOptJson = jsonBooksCollection.find(selector, projector).one[JsObject]

    futOptJson.map(optJson => Ok(optJson.getOrElse(JsObject(Seq()))))
  }

  def getMovies(user_id: Int) = Action.async {

    val selector = Json.obj("user_id" -> 1)

    val projector = Json.obj("_id" -> 0, "user_id" -> 0)

    val futOptJson = jsonMoviesCollection.find(selector, projector).one[JsObject]

    futOptJson.map(optJson => Ok(optJson.getOrElse(JsObject(Seq()))))
  }

  def quotes = Action {
    Ok(views.html.quotes())
  }

  def movies = Action {
    Ok(views.html.movies())
  }

  def books = Action {
    Ok(views.html.books())
  }

}
