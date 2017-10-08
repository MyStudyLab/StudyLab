package models

// Standard Library
import scala.concurrent.Future

// Play Framework
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// Reactive Mongo
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection

// Project
import constructs.{ResultInfo, TodoItem}
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

    todoCollection.flatMap(_.insert(item)).map(
      result =>
        if (result.ok) ResultInfo.succeedWithMessage("used to be result.message")
        else ResultInfo.failWithMessage("failed to add item")
    )

  }

  /**
    *
    * @param username
    * @return
    */
  def getTodoItems(username: String): Future[ResultInfo[List[TodoItem]]] = {

    todoCollection.flatMap(
      _.find(usernameSelector(username)).cursor[TodoItem]().collect[List]().map(
        itemList => ResultInfo.success(s"Retrieved todo items for $username", itemList)
      )
    )
  }

}
