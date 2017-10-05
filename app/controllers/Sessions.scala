package controllers

// Standard Library
import javax.inject.Inject

import scala.concurrent.Future

// Project
import constructs.ResultInfo
import forms._

// Play Framework
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

// Reactive Mongo
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}


/**
  * Controller to manage the study sessions API.
  *
  * @param reactiveMongoApi Holds a reference to the database.
  */
class Sessions @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {

  // Instance of the sessions model
  protected val sessions = new models.Sessions(reactiveMongoApi)

  // Regex defining a valid subject name
  protected val subjectNameRegex = "\\A\\w{1,32}\\z".r

  // Regex defining a valid subject description
  protected val descriptionRegex = "\\A[\\w\\s]{1,256}\\z".r

  /**
    * Invoke the model layer to start a new study session.
    *
    * @return
    */
  def startSession = Action.async { implicit request =>

    withUsername(username => {
      SessionStartForm.startForm.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        goodForm => sessions.startSession(username, goodForm.subject).map(resultInfo =>
          Ok(Json.toJson(resultInfo))
        )
      )
    })
  }


  /**
    * Invoke the model layer to stop the current study session
    *
    * @return
    */
  def stopSession = Action.async { implicit request =>

    withUsername(username => {
      SessionStopForm.stopForm.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        goodForm => sessions.stopSession(username, goodForm.message).map(resultInfo => Ok(Json.toJson(resultInfo)))
      )
    })
  }


  /**
    * Invoke the model layer to abort the current study session.
    *
    * @return
    */
  def cancelSession = Action.async { implicit request =>

    withUsername(username => {
      sessions.abortSession(username).map(resultInfo => Ok(Json.toJson(resultInfo)))
    })

  }


  /**
    * Invoke the model layer to add a subject.
    *
    * @return
    */
  def addSubject = Action.async { implicit request =>

    withUsername(username => {
      AddSubjectForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        goodForm => {

          // Check the subject name
          if (subjectNameRegex.findFirstIn(goodForm.subject).isEmpty) {
            Future(Ok(Json.toJson(ResultInfo.failWithMessage("Unacceptable subject name"))))
          }
          else if (descriptionRegex.findFirstIn(goodForm.description).isEmpty) {
            Future(Ok(Json.toJson(ResultInfo.failWithMessage("Unacceptable subject description"))))
          }
          else {

            // Remove excess whitespace from the subject description
            val cleanedDescription = withoutExcessWhitespace(goodForm.description)

            sessions.addSubject(username, goodForm.subject, cleanedDescription).map(resultInfo => Ok(Json.toJson(resultInfo)))
          }
        }
      )
    })

  }


  /**
    * Invoke the model layer to remove a subject.
    *
    * @return
    */
  def removeSubject = Action.async { implicit request =>

    withUsername(username => {
      RemoveSubjectForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        goodForm => sessions.removeSubject(username, goodForm.subject).map(resultInfo => Ok(Json.toJson(resultInfo)))
      )
    })

  }


  /**
    * Invoke the model layer to rename a subject.
    *
    * @return
    */
  def renameSubject = Action.async { implicit request =>

    withUsername(username => {
      RenameSubjectForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        goodForm => {

          // Check the subject name
          if (subjectNameRegex.findFirstIn(goodForm.newName).isEmpty) {
            Future(Ok(Json.toJson(ResultInfo.failWithMessage("Unacceptable subject name"))))
          } else {

            sessions.renameSubject(username, goodForm.oldName, goodForm.newName).map(resultInfo => Ok(Json.toJson(resultInfo)))
          }
        }
      )
    })
  }


  /**
    * Invoke the model layer to merge subjects.
    *
    * @return
    */
  def mergeSubjects = Action.async { implicit request =>

    withUsername(username => {
      MergeSubjectsForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        goodForm => sessions.mergeSubjects(username, goodForm.absorbed, goodForm.absorbing).map(resultInfo => Ok(Json.toJson(resultInfo)))
      )
    })

  }


  /**
    * Retrieve the session and status data for the given username
    *
    * @param username The username for which to retrieve session data
    * @return
    */
  def sessionsForUsername(username: String) = Action.async { implicit request =>

    sessions.getUserSessionData(username).map(optData => optData.fold(Ok(Json.toJson(ResultInfo.failWithMessage("failed to retrieve sessions"))))(data => Ok(Json.toJson(data))))
  }


  /**
    *
    * @param username
    * @return
    */
  def userStatus(username: String) = Action.async { implicit request =>

    sessions.getUserStatus(username).map(optData => optData.fold(
      Ok(Json.toJson(ResultInfo.failWithMessage("failed to retrieve status")))
    )(data =>
      Ok(Json.obj("success" -> true, "message" -> s"retrieved status for $username", "timestamp" -> System.currentTimeMillis(), "payload" -> data.status))
    )
    )
  }

}
