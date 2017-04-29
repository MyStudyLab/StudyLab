package controllers

// Standard Library
import javax.inject.Inject

// Play Framework
import play.api.mvc._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.twirl.api.Html


/**
  * The Application controller. Handles requests for core pages of site.
  *
  * @param messagesApi
  */
class Application @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  /**
    * Set the navbar according to user status (logged in/out)
    *
    * @param content
    * @return
    */
  protected def setNavbar(pageTitle: String, content: Html) = Action { implicit request =>

    request.session.get("connected").fold(Ok(views.html.siteTemplate(pageTitle)(content)))(username => Ok(views.html.loggedInTemplate(pageTitle)(content)))
  }


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
  def signup = Action { implicit request =>

    if (request.session.get("connected").isEmpty) {
      Ok(views.html.signup())
    } else {
      Redirect("/")
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
      Redirect("/")
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


  /**
    * The Settings Page
    *
    * @return
    */
  def settings(): Action[AnyContent] = setNavbar("Settings", views.html.settings())


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
