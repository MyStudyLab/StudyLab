package controllers

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.play.json._
import play.modules.reactivemongo.json.collection._


class Application @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  def jsonQuotesCollection: JSONCollection = db.collection[JSONCollection]("quotes")


  def home = Action {
    Ok(views.html.home())
  }

  def about = Action {
    Ok(views.html.about())
  }

  def books = Action {
    Ok(views.html.books())
  }

  def movies = Action {
    Ok(views.html.movies())
  }

  def quotes = Action {
    Ok(views.html.quotes())
  }


  def getQuotes(user_id: Int) = Action.async {

    val selector = Json.obj("user_id" -> 1)

    val projector = Json.obj("_id" -> 0, "user_id" -> 0)

    val futOptJson = jsonQuotesCollection.find(selector, projector).one[JsObject]

    futOptJson.map(optJson => Ok(optJson.getOrElse(JsObject(Seq())).value("quotes")))
  }

}
