package controllers

import forms.{SessionStart, SessionStop}
import models.{SessionVector, User, Session, Stats}

import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter}
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

import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONCollection

import reactivemongo.play.json._
import play.modules.reactivemongo.json.collection._


class Application @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  // Determine what type of collection to use
  def jsonUsersCollection: JSONCollection = db.collection[JSONCollection]("users")

  def bsonUsersCollection: BSONCollection = db.collection[BSONCollection]("users")

  def jsonSessionsCollection: JSONCollection = db.collection[JSONCollection]("sessions")

  def bsonSessionsCollection: BSONCollection = db.collection[BSONCollection]("sessions")

  def jsonStatsCollection: JSONCollection = db.collection[JSONCollection]("stats")

  def bsonStatsCollection: BSONCollection = db.collection[BSONCollection]("stats")

  def jsonTextbooksCollection: JSONCollection = db.collection[JSONCollection]("textbooks")

  def bsonTextbooksCollection: BSONCollection = db.collection[BSONCollection]("textbooks")


  def wrapResult(optResult: Option[JsObject], failMessage: String = ""): Result = {

    val failResult = Ok(JsObject(Seq("worked" -> JsBoolean(false), "result" -> JsObject(Seq()), "message" -> JsString(failMessage))))

    optResult.fold(failResult)(result => Ok(JsObject(Seq("worked" -> JsBoolean(true), "result" -> result, "message" -> JsString("")))))
  }

  def create(name: String, age: Int) = Action.async {
    val json = Json.obj(
      "name" -> name,
      "age" -> age,
      "created" -> new java.util.Date().getTime())

    jsonUsersCollection.insert(json).map(writeResult =>
      Ok("Mongo WriteResult: " + writeResult))
  }

  def getByUserId(user_id: Int) = Action.async {

    val query = Json.obj("user_id" -> user_id)

    val futureOption: Future[Option[JsObject]] = jsonUsersCollection.find(query).one[JsObject]

    futureOption.map(opt => wrapResult(opt, failMessage = "Invalid user!"))
  }


  def insertUser(user: User)(implicit writer: BSONDocumentWriter[User]) = Action.async {

    bsonUsersCollection.insert(user).map(writeResult => Ok("Write Result: " + writeResult.getMessage))
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

          optUser.fold(Future(BadRequest("Invalid User")))((user: User) => {

            if (user.status.isStudying) {

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
              bsonUsersCollection.update(selector, modifier, multi = false).map(updateResult => Ok(updateResult.getMessage))
            }
          })
        }
      }
    )

    futResult
  }


  def stop() = Action.async { implicit request =>

    val futResult: Future[Result] = SessionStop.stopForm.bindFromRequest()(request).fold(
      badForm => Future(Ok("Invalid Form")),
      sessionStop => {

        val selector = BSONDocument("user_id" -> sessionStop.user_id)

        val futOptUser: Future[Option[User]] = bsonUsersCollection.find(selector).one[User]

        futOptUser.flatMap { optUser =>

          optUser.fold(Future(Ok("Invalid User")))((user: User) => {

            if (!user.status.isStudying) {

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
              // Could probably throw a few flatMaps in here
              // Can these be done together in an atomic fashion?
              bsonSessionsCollection.update(selector, sessionModifier, multi = false).flatMap(sessionsUpdateResult => {

                if (sessionsUpdateResult.ok) {
                  bsonUsersCollection.update(selector, statusModifier, multi = false).flatMap(userUpdateResult => {
                    if (userUpdateResult.ok) {
                      updateStats(sessionStop.user_id)(request)
                    } else {
                      Future(Ok(""))
                    }
                  })
                } else {
                  Future(Ok(""))
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

  def getUserSessions(user_id: Int) = Action.async {

    val query = Json.obj("user_id" -> user_id)

    val fut: Future[Option[JsObject]] = jsonSessionsCollection.find(query).one[JsObject]

    val futResult: Future[Result] = fut.map(opt => wrapResult(opt, failMessage = "Invalid user!"))

    futResult
  }

  // Should we update all stats whenever we have to pull the session data? Yes
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
        bsonStatsCollection.update(selector, modifier, multi = false).map(updateResult => Ok(updateResult.getMessage))

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
