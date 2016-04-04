package controllers

import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsBoolean, Json}
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import forms.{PasswordAndUserID, SessionStart, SessionStop}

import scala.concurrent.Future


class Sessions @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi, ws: WSClient)
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


  def start() = Action.async { implicit request =>

    SessionStart.startForm.bindFromRequest()(request).fold(
      badForm => Future(Ok("Invalid form")),
      goodForm => sessions.startSession(goodForm.user_id, goodForm.subject).map(a => {
        if (a) {
          Ok(s"now studying ${goodForm.subject}")
        } else {
          Ok("error")
        }
      }))
  }


  def stop() = Action.async { implicit request =>

    SessionStop.stopForm.bindFromRequest()(request).fold(
      badForm => Future(Ok("Invalid form")),
      goodForm => sessions.stopSession(goodForm.user_id).map(a => if (a) Ok("stopped") else Ok("error"))
    )
  }


  def abort() = Action.async { implicit request =>

    SessionStop.stopForm.bindFromRequest()(request).fold(
      badForm => Future(Ok("Invalid form")),
      goodForm => sessions.abortSession(goodForm.user_id).map(a => {
        if (a) {
          Ok("aborted")
        } else {
          Ok("error")
        }
      })
    )
  }

  def update() = Action.async { implicit request =>

    PasswordAndUserID.form.bindFromRequest()(request).fold(
      badForm => Future(Ok("")),
      goodForm => sessions.updateStats(goodForm.user_id).map(a => if (a) Ok("updated") else Ok("error"))
    )
  }


  def startSession() = checked(start)

  def stopSession() = checked(stop)

  def abortSession() = checked(abort)

  def updateSessionStats() = checked(update)

}
