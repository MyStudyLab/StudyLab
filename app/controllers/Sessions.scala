package controllers

// Standard Library
import javax.inject.Inject
import scala.concurrent.Future

// Play Framework
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

// Project
import constructs.ResultInfo
import forms._


/**
  * Controller to manage the study sessions API.
  *
  * @param reactiveMongoApi Holds a reference to the database.
  */
class Sessions @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {

  // Reference to the sessions model
  protected val sessions = new models.Sessions(reactiveMongoApi)

  // Reference to the users model
  protected val users = new models.Users(reactiveMongoApi)

  // Response indicating the request form was invalid.
  protected def invalidFormResponse = Future(Ok(Json.toJson(ResultInfo.invalidForm)))


  /**
    * Check a username and password before performing the given action.
    *
    * @param action The action to perform once the request is authenticated.
    * @tparam A Type parameter of the Action
    * @return
    */
  protected def checked[A](action: Action[A]) = Action.async(action.parser) { implicit request =>

    PasswordAndUsername.form.bindFromRequest()(request).fold(
      badForm => invalidFormResponse,
      goodForm =>
        users.checkCredentials(goodForm.username, goodForm.password).flatMap(matched =>
          if (matched) action(request)
          else Future(Ok(Json.toJson(ResultInfo.badUsernameOrPass)))
        )
    )
  }


  /**
    * Invoke the model layer to start a new study session.
    *
    * @return
    */
  protected def start = Action.async { implicit request =>

    SessionStartForm.startForm.bindFromRequest()(request).fold(
      badForm => invalidFormResponse,
      goodForm => sessions.startSession(goodForm.username, goodForm.subject).map(resultInfo => Ok(Json.toJson(resultInfo)))
    )
  }


  /**
    * Invoke the model layer to stop the current study session
    *
    * @return
    */
  protected def stop = Action.async { implicit request =>

    SessionStopForm.stopForm.bindFromRequest()(request).fold(
      badForm => invalidFormResponse,
      goodForm => sessions.stopSession(goodForm.username, goodForm.message).map(resultInfo => Ok(Json.toJson(resultInfo)))
    )
  }


  /**
    * Invoke the model layer to abort the current study session.
    *
    * @return
    */
  protected def abort = Action.async { implicit request =>

    SessionStopForm.stopForm.bindFromRequest()(request).fold(
      badForm => invalidFormResponse,
      goodForm => sessions.abortSession(goodForm.username).map(resultInfo => Ok(Json.toJson(resultInfo)))
    )
  }


  /**
    * Invoke the model layer to add a subject.
    *
    * @return
    */
  protected def add = Action.async { implicit request =>

    AddOrRemoveSubjectForm.form.bindFromRequest()(request).fold(
      badForm => invalidFormResponse,
      goodForm => sessions.addSubject(goodForm.username, goodForm.subject, goodForm.description).map(resultInfo => Ok(Json.toJson(resultInfo)))
    )
  }


  /**
    * Invoke the model layer to remove a subject.
    *
    * @return
    */
  protected def remove = Action.async { implicit request =>

    AddOrRemoveSubjectForm.form.bindFromRequest()(request).fold(
      badForm => invalidFormResponse,
      goodForm => sessions.removeSubject(goodForm.username, goodForm.subject).map(resultInfo => Ok(Json.toJson(resultInfo)))
    )
  }


  /**
    * Invoke the model layer to rename a subject.
    *
    * @return
    */
  protected def rename = Action.async { implicit request =>

    RenameSubjectForm.form.bindFromRequest()(request).fold(
      badForm => invalidFormResponse,
      goodForm => sessions.renameSubject(goodForm.username, goodForm.oldName, goodForm.newName).map(resultInfo => Ok(Json.toJson(resultInfo)))
    )
  }


  /**
    * Invoke the model layer to merge subjects.
    *
    * @return
    */
  protected def merge = Action.async { implicit request =>

    MergeSubjectsForm.form.bindFromRequest()(request).fold(
      badForm => invalidFormResponse,
      goodForm => sessions.mergeSubjects(goodForm.username, goodForm.absorbed, goodForm.absorbing).map(resultInfo => Ok(Json.toJson(resultInfo)))
    )
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
    * Start a study session
    *
    * @return
    */
  def startSession() = checked(start)

  /**
    * Stop a study session
    *
    * @return
    */
  def stopSession() = checked(stop)

  /**
    * Abort the current study session
    *
    * @return
    */
  def abortSession() = checked(abort)

  /**
    * Add a subject to a user's subject list
    *
    * @return
    */
  def addSubject() = checked(add)

  /**
    * Remove a subject from a user's subject list
    *
    * @return
    */
  def removeSubject() = checked(remove)

  /**
    * Rename one of the subjects in a user's subject list
    *
    * @return
    */
  def renameSubject() = checked(rename)

  /**
    * Merge two subjects from a user's subject list
    *
    * @return
    */
  def mergeSubjects() = checked(merge)

}
