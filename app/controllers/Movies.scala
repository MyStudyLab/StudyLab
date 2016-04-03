package controllers

import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsBoolean, JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.api.libs.concurrent.Execution.Implicits.defaultContext


class Movies @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi, ws: WSClient)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  val movies = new models.Movies(reactiveMongoApi)

  val failResponse = Json.obj("response" -> JsBoolean(false))


  def getMovies(user_id: Int) = Action.async {

    movies.getAllJson(user_id).map(a => Ok(a.getOrElse(failResponse)))
  }

  def updateMovieStats(user_id: Int) = Action.async {

    movies.updateStats(user_id).map(a => Ok(a.toString))
  }
}
