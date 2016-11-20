package controllers

// Standard Library
import javax.inject.Inject

// Play Framework
import play.api.mvc._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}


/**
  *
  * @param reactiveMongoApi
  * @param messagesApi
  */
class Application @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  /**
    * The Home Page
    */
  def home = Action {
    Ok(views.html.home())
  }


  /**
    * My resume as a PDF
    *
    * @return
    */
  def resume = controllers.Assets.at(path = "/public", file = "pdfs/resume.pdf")

  /**
    * The About Page
    */
  def about = Action {
    Ok(views.html.about())
  }

  /**
    * The Quotes Page
    */
  def quotes = Action {
    Ok(views.html.quotes())
  }

  def profiles = Action {
    Ok(views.html.profiles())
  }

}
