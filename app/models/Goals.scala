package models

// Standard Library
import constructs.responses.{StatusSubjects, StatusSubjectsSessions}
import scala.concurrent.Future

// Play Framework
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// Reactive Mongo
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}

// Project
import constructs.{CumulativeGoal, ResultInfo}
import helpers.Selectors.usernameSelector

/**
  * Model layer to manage goals
  *
  * @param mongoApi
  */
class Goals(protected val mongoApi: ReactiveMongoApi) {


  /**
    *
    * @param username The username for which to add the goal
    * @param goal
    * @return
    */
  def addCumulativeGoal(username: String, goal: CumulativeGoal): Future[ResultInfo] = {

    def cumGoals: BSONCollection = mongoApi.db.collection("cumulative_goals")

    cumGoals.insert(goal).map(result => ResultInfo(result.ok, result.message, System.currentTimeMillis()))

  }

}
