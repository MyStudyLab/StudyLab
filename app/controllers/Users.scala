package controllers

import javax.inject.Inject
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import forms._


class Users @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {



}
