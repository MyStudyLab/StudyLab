package controllers

import java.time.{ZoneId, ZonedDateTime}

import forms.{AddMovieForm, SessionStart, SessionStop, UpdateForm}
import models.{Session, _}
import reactivemongo.bson.{BSONBoolean, BSONDocument}

import scala.concurrent.Future
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import javax.inject.Inject

import play.api.libs.ws.WSClient
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.play.json._
import play.modules.reactivemongo.json.collection._


class Application @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi, ws: WSClient)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  def jsonUsersCollection: JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("users")

  def bsonUsersCollection: BSONCollection = db.collection[BSONCollection]("users")

  def bsonSessionsCollection: BSONCollection = db.collection[BSONCollection]("sessions")

  def jsonQuotesCollection: JSONCollection = db.collection[JSONCollection]("quotes")

  def bsonBooksCollection: BSONCollection = db.collection[BSONCollection]("books")

  def jsonBooksCollection: JSONCollection = db.collection[JSONCollection]("books")

  def bsonMoviesCollection: BSONCollection = db.collection[BSONCollection]("movies")


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
                "stats" -> SessionStats.stats(user.sessions)
              )
            )

            // Update the stats
            bsonSessionsCollection.update(selector, modifier, multi = false).map(updateResult => {
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

            if (collection == "books") {
              updateBooks(goodForm.user_id)(request)
            } else {
              Future(Ok("Invalid collection"))
            }
          }
        })

      }
    })
  }


  // Use WebServices to get data from OMDb
  def addMovie() = Action.async { implicit request =>

    AddMovieForm.form.bindFromRequest.fold(badForm => Future(Ok("")), goodForm => {

      val req = s"http://www.omdbapi.com/?i=${goodForm.imdbID}&plot=full&r=json"

      ws.url(req)
    })


    ???
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
