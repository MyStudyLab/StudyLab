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
  def home() = Action { implicit request =>

    request.session.get("connected").fold(Ok(views.html.home()))(username => Ok(views.html.loggedInHome(username)))
  }

  /**
    * The Sign-Up Page
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
  def loginPage = Action {
    Ok(views.html.login())
  }


  /**
    * Logout and return to the Home Page
    *
    * @return
    */
  def logout = Action {
    Redirect(routes.Application.home()).withNewSession
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
