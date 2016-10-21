package controllers

import javax.inject.Inject

import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import forms._
import play.api.libs.json.Json


/**
  * Controller to manage study sessions.
  *
  * @param reactiveMongoApi Holds a reference to the database.
  */
class Sessions @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {

  // Sessions model
  val sessions = new models.Sessions(reactiveMongoApi)

  // User model
  val users = new models.Users(reactiveMongoApi)

  // Response indicating the request form was invalid.
  val invalidForm = Future(Ok("Invalid form."))


  /**
    * Check a username and password before performing the given action.
    *
    * @param action The action to perform once the request is authenticated.
    * @tparam A
    * @return
    */
  def checked[A](action: Action[A]) = Action.async(action.parser) { implicit request =>

    PasswordAndUsername.form.bindFromRequest()(request).fold(
      badForm => invalidForm,
      goodForm =>
        users.checkPassword(goodForm.username, goodForm.password).flatMap(matched =>
          if (matched) action(request)
          else Future(Ok("Bad username or password."))
        )
    )
  }


  /**
    * Invoke the model layer to start a new study session.
    *
    * @return
    */
  def start = Action.async { implicit request =>

    SessionStartForm.startForm.bindFromRequest()(request).fold(
      badForm => invalidForm,
      goodForm => sessions.startSession(goodForm.username, goodForm.subject).map(resultInfo => Ok(resultInfo.message))
    )
  }


  /**
    * Invoke the model layer to stop the current study session
    *
    * @return
    */
  def stop = Action.async { implicit request =>

    SessionStopForm.stopForm.bindFromRequest()(request).fold(
      badForm => invalidForm,
      goodForm => sessions.stopSession(goodForm.username, goodForm.message).map(resultInfo => Ok(resultInfo.message))
    )
  }


  /**
    * Invoke the model layer to abort the current study session.
    *
    * @return
    */
  def abort = Action.async { implicit request =>

    SessionStopForm.stopForm.bindFromRequest()(request).fold(
      badForm => invalidForm,
      goodForm => sessions.abortSession(goodForm.username).map(a => Ok(a.message))
    )
  }


  /**
    * Invoke the model layer to add a subject.
    *
    * @return
    */
  def add = Action.async { implicit request =>

    AddOrRemoveSubjectForm.form.bindFromRequest()(request).fold(
      badForm => invalidForm,
      goodForm => sessions.addSubject(goodForm.username, goodForm.subject, goodForm.description).map(a => Ok(a.message))
    )
  }


  /**
    * Invoke the model layer to remove a subject.
    *
    * @return
    */
  def remove = Action.async { implicit request =>

    AddOrRemoveSubjectForm.form.bindFromRequest()(request).fold(
      badForm => invalidForm,
      goodForm => sessions.removeSubject(goodForm.username, goodForm.subject).map(a => Ok(a.message))
    )
  }


  /**
    * Invoke the model layer to rename a subject.
    *
    * @return
    */
  def rename = Action.async { implicit request =>

    RenameSubjectForm.form.bindFromRequest()(request).fold(
      badForm => invalidForm,
      goodForm => sessions.renameSubject(goodForm.username, goodForm.oldName, goodForm.newName).map(a => Ok(a.message))
    )
  }


  /**
    * Invoke the model layer to merge subjects.
    *
    * @return
    */
  def merge = Action.async { implicit request =>

    MergeSubjectsForm.form.bindFromRequest()(request).fold(
      badForm => invalidForm,
      goodForm => sessions.mergeSubjects(goodForm.username, goodForm.absorbed, goodForm.absorbing).map(a => Ok(a.message))
    )
  }


  def getStats(username: String) = Action.async { implicit request =>

    // Return the result with the current time in the users timezone
    sessions.getStats(username).map(optStats => optStats.fold(Ok(Json.obj("success" -> false)))(stats =>
      Ok(Json.toJson(stats))))
  }


  def getUserSessionData(username: String) = Action.async { implicit request =>

    sessions.getUserSessionData(username).map(optData => optData.fold(Ok(Json.obj("success" -> false)))(data => Ok(Json.toJson(data))))
  }


  def startSession() = checked(start)

  def stopSession() = checked(stop)

  def abortSession() = checked(abort)

  def addSubject() = checked(add)

  def removeSubject() = checked(remove)

  def renameSubject() = checked(rename)

  def mergeSubjects() = checked(merge)

}
