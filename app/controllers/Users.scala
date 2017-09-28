package controllers

// Standard Library
import javax.inject.Inject

import forms.LoginForm
import play.api.i18n.{I18nSupport, MessagesApi}

import scala.concurrent.Future

// Project
import constructs.{ResultInfo, User}
import forms.AddUserForm

// Play Framework
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

// Reactive Mongo
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

/**
  *
  * @param reactiveMongoApi Holds a reference to the database.
  */
class Users @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {


  /**
    * Instance of the Users model
    */
  protected val usersModel = new models.Users(reactiveMongoApi)

  /**
    * Begin a new session
    *
    * @return
    */
  def login = Action.async { implicit request =>
    LoginForm.form.bindFromRequest()(request).fold(
      _ => invalidFormResponse,
      goodForm => {
        usersModel.checkCredentials(goodForm.username, goodForm.password).map(valid =>

          if (valid) {
            Redirect(routes.Application.home()).withSession("connected" -> goodForm.username)
          } else {
            Ok(views.html.login())
          })
      }
    )
  }


  /**
    * Add a new user via a POSTed html form
    *
    * @return
    */
  def addNewUser = Action.async { implicit request =>

    AddUserForm.form.bindFromRequest()(request).fold(
      _ => invalidFormResponse,
      goodForm => {

        // Define the form of a valid username
        val usernameRegex = "\\A\\w{1,32}\\z".r

        // Define the form of a valid password
        val passwordRegex = "\\A\\w{8,32}\\z".r

        // Check the username
        if (usernameRegex.findFirstIn(goodForm.username).isEmpty) {
          Future(Ok(Json.toJson(ResultInfo.failWithMessage("Unacceptable username"))))
        }

        // Check the password
        else if (passwordRegex.findFirstIn(goodForm.password).isEmpty) {
          Future(Ok(Json.toJson(ResultInfo.failWithMessage("Unacceptable password"))))
        }

        else {
          try {

            // Create User object representing the new user
            val newUser = User(goodForm.username, goodForm.firstName, goodForm.lastName, goodForm.email, goodForm.password)

            // Insert the User object into the database
            usersModel.addNewUser(newUser).map(
              resultInfo => Ok(Json.toJson(resultInfo))
            )

          } catch {
            case _: Throwable => Future(Ok(Json.toJson(ResultInfo.failWithMessage("Error adding new user"))))
          }
        }
      }
    )
  }


  /**
    * Remove a user
    *
    * @return
    */
  def removeUser = Action.async { implicit request =>
    ???
  }


  /**
    * Check a user's credentials
    *
    * @return
    */
  def checkCredentials = Action.async { implicit request =>

    LoginForm.form.bindFromRequest()(request).fold(
      _ => invalidFormResponse,
      goodForm => {
        usersModel.checkCredentials(goodForm.username, goodForm.password).map(valid =>

          if (valid) {
            Ok(Json.obj("success" -> true, "message" -> "<none>", "timestamp" -> System.currentTimeMillis(), "payload" -> valid)).withSession("connected" -> goodForm.username)
          } else {
            Ok(Json.obj("success" -> true, "message" -> "<none>", "timestamp" -> System.currentTimeMillis(), "payload" -> valid))
          })
      }
    )
  }


  /**
    * Get the profiles for the given user
    *
    * @param username The username for which to retrieve data
    * @return
    */
  def profilesForUsername(username: String): Action[AnyContent] = Action.async { implicit request =>

    usersModel.socialProfiles(username).map(
      optData => optData.fold(Ok(Json.toJson(ResultInfo.failWithMessage("failed to retrieve profiles"))))(data => Ok(Json.toJson(data)))
    )
  }


  /**
    * Get the about message for the given user
    *
    * @param username The username for which to retrieve data
    * @return
    */
  def aboutMessage(username: String): Action[AnyContent] = Action.async { implicit request =>

    usersModel.aboutMessage(username).map(
      optData => optData.fold(Ok(Json.toJson(ResultInfo.failWithMessage("failed to retrieve about message"))))(data => Ok(Json.toJson(data)))
    )
  }


  /**
    * Set the about message for the given user
    *
    * @param username The username for which to set the about message
    * @param message  The new about message
    * @return
    */
  def updateAboutMessage(username: String, message: String): Action[AnyContent] = Action.async { implicit request =>

    val aboutRegex = "^[\\w\\s]{0,256}$".r

    // Check the about message
    if (aboutRegex.findFirstIn(message).isEmpty) {
      Future(Ok(Json.toJson(ResultInfo.failWithMessage("Unacceptable 'about' text"))))
    }

    val cleanedMessage = "\\s+".r.replaceAllIn(message, " ")

    ???
  }

}
