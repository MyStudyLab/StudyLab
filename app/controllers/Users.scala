package controllers

// Standard Library
import javax.inject.Inject

import forms.LoginForm

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
class Users @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {


  /**
    * Instance of the Users model
    */
  protected val usersModel = new models.Users(reactiveMongoApi)


  /**
    * Add a new user via a POSTed html form
    *
    * @return
    */
  def addNewUser = Action.async { implicit request =>

    AddUserForm.form.bindFromRequest()(request).fold(
      _ => invalidFormResponse,
      goodForm => {

        // User object representing the new user
        val newUser = User(goodForm.username, goodForm.firstName, goodForm.lastName, goodForm.email, goodForm.password)

        // Insert the user object into the database
        usersModel.addNewUser(newUser).map(
          resultInfo => Ok(Json.toJson(resultInfo))
        )
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
    *
    * @return
    */
  def checkCredentials = Action.async { implicit request =>

    LoginForm.form.bindFromRequest()(request).fold(
      _ => invalidFormResponse,
      goodForm => {
        usersModel.checkCredentials(goodForm.username, goodForm.password).map(valid => Ok(Json.obj("success" -> true, "message" -> "<none>", "timestamp" -> System.currentTimeMillis(), "payload" -> valid)))
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

}
