package controllers

import models.{SessionVector, SessionStart, User, Session, Stats, Textbook}

import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONArray}
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

import scala.util.{Success, Failure}

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


  def startSession(user_id: Int) = Action.async { implicit request =>

    // Send start info to database
    // What is the best db design?

    val startForm = SessionStart.startForm

    val selector = BSONDocument("user_id" -> user_id)

    val futOptUser: Future[Option[User]] = bsonUsersCollection.find(selector).one[User]

    val futResult: Future[Result] = futOptUser.flatMap { optUser =>

      optUser.fold(Future(wrapResult(None, failMessage = "Invalid user!")))((user: User) => {

        val status = user.status

        if (!status.isStudying) {
          startForm.bindFromRequest.fold(
            badForm => {
              Future(wrapResult(None, failMessage = "Errors in form"))
            },
            sessionStart => {

              val modifier = BSONDocument(
                "$currentDate" -> BSONDocument(
                  "status.start" -> true
                ),
                "$set" -> BSONDocument(
                  "status.isStudying" -> true,
                  "status.subject" -> sessionStart.subject
                )
              )

              // Check that subject is valid for the user
              if (!user.subjects.contains(sessionStart.subject)) {
                Future(wrapResult(None, failMessage = "Invalid subject!"))
              } else {
                // Convert the update result to a JSON
                bsonUsersCollection.update(selector, modifier, multi = false).map(updateResult => Ok(updateResult.getMessage))
              }
            }
          )
        } else {
          Future(wrapResult(None, failMessage = "Already studying!"))
        }
      })

    }

    futResult
  }


  def stopSession(user_id: Int) = Action.async { implicit request =>

    val selector = BSONDocument("user_id" -> user_id)

    val futOptUser: Future[Option[User]] = bsonUsersCollection.find(selector).one[User]

    val futResult: Future[Result] = futOptUser.flatMap { optUser =>

      optUser.fold(Future(wrapResult(None, failMessage = "Invalid user!")))(user => {

        // We need to check the user's current status
        val status = user.status

        // Check if the user is currently studying
        if (status.isStudying) {

          // Modifier to add the new session
          val sessionModifier = BSONDocument(
            "$push" -> BSONDocument(
              "sessions" -> Session(status.start, System.currentTimeMillis(), status.subject)
            )
          )

          // Modifier to update the user's status
          val statusModifier = BSONDocument(
            "$set" -> BSONDocument(
              "status.isStudying" -> false,
              "status.subject" -> "",
              "status.start" -> new java.util.Date(0)
            )
          )

          // Update the database with the new session
          bsonSessionsCollection.update(selector, sessionModifier, multi = false).map(updateResult => Ok(updateResult.getMessage))
          bsonUsersCollection.update(selector, statusModifier, multi = false).map(updateResult => Ok(updateResult.getMessage))
        } else {

          // If the user wasn't studying, they can't end a session.
          Future(wrapResult(None, failMessage = "You are not studying at the moment."))
        }
      })
    }

    // Return our future result
    futResult
  }


  def getUserSessions(user_id: Int) = Action.async {

    val query = Json.obj("user_id" -> user_id)

    val fut: Future[Option[JsObject]] = jsonSessionsCollection.find(query).one[JsObject]

    val futResult: Future[Result] = fut.map(opt => wrapResult(opt, failMessage = "Invalid user!"))

    futResult
  }

  // Should we update all stats whenever we have to pull the session data? Yes
  def updateStats(user_id: Int) = Action.async {

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


  // TODO:  Check the lastUpdate timestamp here
  def getStats(user_id: Int) = Action.async {

    val selector = Json.obj("user_id" -> user_id)

    // Don't send the object id
    val projector = Json.obj("_id" -> 0)

    val futOptJson: Future[Option[JsObject]] = jsonStatsCollection.find(selector, projector).one[JsObject]

    val futResult: Future[Result] = futOptJson.map(opt => wrapResult(opt, failMessage = "Invalid user!"))

    futResult
  }

  def index = Action {
    Ok(views.html.index())
  }

}
