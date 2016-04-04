package controllers

import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.ws.WSClient
import play.api.mvc.Controller
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}


class Users @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi, ws: WSClient)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

}
