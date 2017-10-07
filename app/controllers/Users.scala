package controllers

// Standard Library
import javax.inject.Inject

import forms.{DeactivateProfileForm, LoginForm, UpdateEmailForm, UpdatePasswordForm}
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
          Future(Ok(ResultInfo.failWithMessage("Unacceptable username").toJson))
        }

        // Check the password
        else if (passwordRegex.findFirstIn(goodForm.password).isEmpty) {
          Future(Ok(ResultInfo.failWithMessage("Unacceptable password").toJson))
        }

        else {
          try {

            // Create User object representing the new user
            val newUser = User(goodForm.username, goodForm.firstName, goodForm.lastName, goodForm.email, goodForm.password)

            // Insert the User object into the database
            usersModel.addNewUser(newUser).map(
              resultInfo => Ok(resultInfo.toJson)
            )

          } catch {
            case _: Throwable => Future(Ok(ResultInfo.failWithMessage("Error adding new user").toJson))
          }
        }
      }
    )
  }


  /**
    * Remove a user from the system
    *
    * @return
    */
  def deleteUser = Action.async { implicit request =>

    withUsername(username => {
      DeactivateProfileForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        goodForm => {
          usersModel.deleteUser(username, goodForm.password).map(result => if (result.success) Redirect(routes.Application.home()).withNewSession else InternalServerError(result.toJson))
        }
      )

    })
  }


  /**
    * Update a user's email address
    *
    * @return
    */
  def updateEmail = Action.async { implicit request =>

    withUsername(username => {
      UpdateEmailForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        goodForm => {
          usersModel.changeEmail(username, goodForm.email).map(success =>
            if (success) Ok(ResultInfo.succeedWithMessage("Email address has been updated").toJson)
            else InternalServerError(ResultInfo.failWithMessage("Failed to update email address").toJson))
        }
      )
    })
  }


  /**
    * Update a user's email password
    *
    * @return
    */
  def updatePassword = Action.async { implicit request =>

    withUsername(username => {
      UpdatePasswordForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        goodForm => {
          usersModel.changePassword(username, goodForm.password).map(success =>
            if (success) Ok(ResultInfo.succeedWithMessage("Password has been updated").toJson)
            else InternalServerError(ResultInfo.failWithMessage("Failed to update password").toJson))
        }
      )
    })
  }


  /**
    * Get the profiles for the given user
    *
    * @param username The username for which to retrieve data
    * @return
    */
  def profilesForUsername(username: String): Action[AnyContent] = Action.async { implicit request =>

    usersModel.socialProfiles(username).map(
      optData => optData.fold(Ok(ResultInfo.failWithMessage("failed to retrieve profiles").toJson))(data => Ok(Json.toJson(data)))
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
      optData => optData.fold(Ok(ResultInfo.failWithMessage("failed to retrieve about message").toJson))(data => Ok(Json.toJson(data)))
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
      Future(Ok(ResultInfo.failWithMessage("Unacceptable 'about' text").toJson))
    }

    val cleanedMessage = "\\s+".r.replaceAllIn(message, " ")

    ???
  }

}
