package models

// Standard Library
import constructs.TodoItem
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
  * Model layer to manage to-do lists
  *
  * @param mongoApi
  */
class Todo(protected val mongoApi: ReactiveMongoApi) {

  protected def todoCollection: Future[BSONCollection] = mongoApi.database.map(_.collection("todo_items"))

  /**
    *
    * @param username The username for which to add the goal
    * @param item
    * @return
    */
  def addTodoItem(username: String, item: TodoItem): Future[ResultInfo[String]] = {

    // TODO: Replace the message field
    todoCollection.flatMap(_.insert(item)).map(result => ResultInfo(result.ok, "used to be result.message"))

  }

}
