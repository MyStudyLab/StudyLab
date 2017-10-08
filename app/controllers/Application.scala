package controllers

// Standard Library
import javax.inject.Inject

import scala.concurrent.Future

// Play Framework
import play.api.mvc._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.twirl.api.Html


/**
  * The Application controller. Handles requests for core pages of site.
  *
  * @param messagesApi For I18n
  */
class Application @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  /**
    * Set the navbar according to user status (logged in/out)
    *
    * @param content The main content of the page
    * @return
    */
  protected def setNavbar(pageTitle: String, content: Html) = Action { implicit request =>

    request.session.get("connected").fold(Ok(views.html.siteTemplate(pageTitle)(content)))(username => Ok(views.html.loggedInTemplate(pageTitle)(play.twirl.api.Html(""))(content)))
  }


  /**
    * The Home Page
    *
    * @return
    */
  def home = Action { implicit request =>

    request.session.get("connected").fold(Ok(views.html.home()))(username => Ok(views.html.loggedInHome(username)))
  }


  /**
    * The Sign-Up Page
    *
    * @return
    */
  def signup = Action { implicit request =>

    if (request.session.get("connected").isEmpty) {
      Ok(views.html.signup())
    } else {
      Redirect(routes.Application.home())
    }
  }


  /**
    * The Login Page
    *
    * @return
    */
  def loginPage = Action { implicit request =>

    if (request.session.get("connected").isEmpty) {
      Ok(views.html.login())
    } else {
      Redirect(routes.Application.home())
    }

  }


  /**
    * Logout and return to the Home Page
    *
    * @return
    */
  def logout = Action {
    Redirect(routes.Application.home()).withNewSession
  }


  def todo = Action { implicit request =>

    withUsername(username => Ok(views.html.todo(username)))
  }

  /**
    * The Journal page
    *
    * @return
    */
  def journal = Action { implicit request =>

    withUsername(username => Ok(views.html.journal(username)))
  }


  /**
    * The Settings Page
    *
    * @return
    */
  def settings() = Action { implicit request =>

    withUsername(username => Ok(views.html.settings(username)))
  }

  /**
    * The About Page
    *
    * @return
    */
  def about: Action[AnyContent] = setNavbar("About", views.html.aboutPageBody())


  /**
    * The Contact Page
    *
    * @return
    */
  def contact(): Action[AnyContent] = setNavbar("Contact", views.html.contact())


  /**
    * The Help Page
    *
    * @return
    */
  def help(): Action[AnyContent] = setNavbar("Help", views.html.help())
}
