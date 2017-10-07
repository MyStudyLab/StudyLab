package controllers

// Standard Library
import javax.inject.Inject

import constructs.CumulativeGoal

// Project
import constructs.ResultInfo
import forms._

// Play Framework
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

// Reactive Mongo
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}


/**
  * Controller to manage the study sessions API.
  *
  * @param reactiveMongoApi Holds a reference to the database.
  */
class Goals @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {

  protected val goals = new models.Goals(reactiveMongoApi)

  /**
    * Add a cumulative-goal for the given username
    *
    * @param username
    * @return
    */
  def addCumulativeGoal(username: String) = Action.async { implicit request =>
    goals.addCumulativeGoal(username, CumulativeGoal("", 0, 0, 0, Vector[Int]())).map(result => Ok(result.toJson))
  }
}
