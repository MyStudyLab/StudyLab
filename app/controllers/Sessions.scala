package controllers

import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import forms.{AddSubjectForm, PasswordAndUserID, SessionStart, SessionStop}
import helpers.ResultInfo

import scala.concurrent.Future


class Sessions @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  val sessions = new models.Sessions(reactiveMongoApi)

  val users = new models.Users(reactiveMongoApi)

  val failResponse = Json.obj("response" -> JsBoolean(false))


  def getStats(user_id: Int) = Action.async { implicit request =>

    // Return the result with the current time in the users timezone
    sessions.getStatsAsJSON(user_id).map(a => Ok(a.getOrElse(failResponse)))
  }


  def checked[A](action: Action[A]) = Action.async(action.parser) { implicit request =>

    PasswordAndUserID.form.bindFromRequest()(request).fold(
      badForm => Future(Ok("Invalid Form")),
      goodForm => {

        users.checkPassword(goodForm.user_id, goodForm.password).flatMap(matched => {
          if (matched) {
            action(request)
          } else {
            Future(Ok(""))
          }
        })

      }
    )
  }


  /**
    * Invoke the model layer to start a new study session.
    *
    * @return
    */
  def start = Action.async { implicit request =>

    SessionStart.startForm.bindFromRequest()(request).fold(
      badForm => Future(Ok("Invalid form")),
      goodForm => sessions.startSession(goodForm.user_id, goodForm.subject).map(resultInfo => Ok(resultInfo.message))
    )
  }


  /**
    * Invoke the model layer to stop the current study session
    *
    * @return
    */
  def stop = Action.async { implicit request =>

    SessionStop.stopForm.bindFromRequest()(request).fold(
      badForm => Future(Ok("Invalid form")),
      goodForm => sessions.stopSession(goodForm.user_id).map(resultInfo => Ok(resultInfo.message))
    )
  }


  /**
    * Invoke the model layer to abort the current study session.
    *
    * @return
    */
  def abort = Action.async { implicit request =>

    SessionStop.stopForm.bindFromRequest()(request).fold(
      badForm => Future(Ok("Invalid form")),
      goodForm => sessions.abortSession(goodForm.user_id).map(a => Ok(a.message))
    )
  }


  /**
    * Invoke the model layer to add a new study subject.
    *
    * @return
    */
  def add = Action.async { implicit request =>

    AddSubjectForm.form.bindFromRequest()(request).fold(
      badForm => Future(Ok("Invalid form")),
      goodForm => sessions.addSubject(goodForm.user_id, goodForm.subject).map(a => {
        if (a) {
          Ok(s"added ${goodForm.subject} to subject list")
        } else {
          Ok("error")
        }
      })
    )
  }


  def startSession() = checked(start)

  def stopSession() = checked(stop)

  def abortSession() = checked(abort)

  def addSubject() = checked(add)

}
