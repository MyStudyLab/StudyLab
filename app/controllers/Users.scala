package controllers

// Standard Library
import javax.inject.Inject

// Project
import constructs.ResultInfo

// Play Framework
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json


/**
  *
  * @param reactiveMongoApi Holds a reference to the database.
  */
class Users @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {


  /**
    * Instance of the Users model
    */
  protected val users = new models.Users(reactiveMongoApi)


  /**
    * Get the profiles for the given user
    *
    * @param username The username for which to retrieve data
    * @return
    */
  def profilesForUsername(username: String): Action[AnyContent] = Action.async { implicit request =>

    users.socialProfiles(username).map(optData => optData.fold(Ok(Json.toJson(ResultInfo.failWithMessage("failed to retrieve profiles"))))(data => Ok(Json.toJson(data))))
  }

  /**
    * Get the about message for the given user
    *
    * @param username The username for which to retrieve data
    * @return
    */
  def aboutMessage(username: String): Action[AnyContent] = Action.async { implicit request =>

    users.aboutMessage(username).map(optData => optData.fold(Ok(Json.toJson(ResultInfo.failWithMessage("failed to retrieve about message"))))(data => Ok(Json.toJson(data))))
  }

}
