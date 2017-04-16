package controllers

// Standard Library
import javax.inject.Inject

// Play Framework
import play.api.mvc._
import play.api.i18n.{I18nSupport, MessagesApi}


/**
  *
  * @param messagesApi
  */
class Application @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  /**
    * The Home Page
    *
    * @return
    */
  def home() = Action {
    Ok(views.html.home())
  }

  /**
    * The Signup Page
    *
    * @return
    */
  def signup = Action {
    Ok(views.html.signup())
  }

  /**
    * The Login Page
    *
    * @return
    */
  def login = Action {
    Ok(views.html.login())
  }

  /**
    * The Home Page
    *
    * @return
    */
  def dashboard(username: String) = Action {
    Ok(views.html.dashboard(username))
  }

  /**
    * The About Page
    *
    * @return
    */
  def about(username: String) = Action {
    Ok(views.html.about(username))
  }


  /**
    * The Profiles Page
    *
    * @return
    */
  def profiles = Action {
    Ok(views.html.profiles())
  }

}
