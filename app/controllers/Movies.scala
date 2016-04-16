package controllers

import javax.inject.Inject

import forms.{AddMovieForm, PasswordAndUserID}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsBoolean, JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future


class Movies @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi, ws: WSClient)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  val movies = new models.Movies(reactiveMongoApi)

  val users = new models.Users(reactiveMongoApi)

  val failResponse = Json.obj("response" -> JsBoolean(false))


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

  // Use WebServices to get data from OMDb
  def addMovie() = Action.async { implicit request =>

    AddMovieForm.form.bindFromRequest.fold(badForm => Future(Ok("")), goodForm => {

      val req = s"http://www.omdbapi.com/?i=${goodForm.imdbID}&plot=full&r=json"

      ws.url(req)
    })


    ???
  }

  def update() = Action.async { implicit request =>

    PasswordAndUserID.form.bindFromRequest()(request).fold(
      badForm => Future(Ok("")),
      goodForm => movies.updateStats(goodForm.user_id).map(a => if (a) Ok("updated") else Ok("error"))
    )
  }

  def getMovies(user_id: Int) = Action.async {

    movies.getAllJson(user_id).map(a => Ok(a.getOrElse(failResponse)))
  }

  def updateMovieStats() = checked(update)
}