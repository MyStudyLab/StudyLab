package controllers

import javax.inject.Inject

import constructs.ResultInfo
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
    *
    * @param username The username for which to retrieve data
    * @return
    */
  def profilesForUsername(username: String) = Action.async { implicit request =>

    users.socialProfiles(username).map(optData => optData.fold(Ok(Json.toJson(ResultInfo.failWithMessage("failed to retrieve profiles"))))(data => Ok(Json.toJson(data))))
  }

}
