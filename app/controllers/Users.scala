package controllers

// Standard Library
import javax.inject.Inject

import forms.AddUserForm

// Project
import constructs.ResultInfo

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
      badForm => invalidFormResponse,
      goodForm => usersModel.addNewUser(goodForm.username, goodForm.email, goodForm.password).map(resultInfo => Ok(Json.toJson(resultInfo)))
    )
  }


  /**
    * Add a new user via query parameters
    *
    * @param username The new user's username
    * @param email    The new user's email
    * @param password The new user's password
    * @return
    */
  def addNewUserFromParams(username: String, email: String, password: String) = Action.async { implicit request =>

    usersModel.addNewUser(username, email, password).map(result => Ok(Json.toJson(result)))
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
