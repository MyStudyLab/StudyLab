package controllers

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.api.libs.concurrent.Execution.Implicits.defaultContext


class Books @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {


  val books = new models.Books(reactiveMongoApi)

  val users = new models.Users(reactiveMongoApi)

  val failResponse = Json.obj("response" -> JsBoolean(false))

  def getBooks(user_id: Int) = Action.async {

    books.getStatsAsJSON(user_id).map(a => Ok(a.getOrElse(failResponse)))
  }

}
