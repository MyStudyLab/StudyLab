package models

// Standard Library
import constructs.Point
import play.api.libs.json._
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future

// Play Framework
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// Reactive Mongo
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.api.collections.bson.BSONCollection

// Project
import constructs.{ResultInfo, TodoItem}
import helpers.Selectors.usernameAndID

/**
  * Model layer to manage to-do lists
  *
  * @param mongoApi
  */
class Todo(protected val mongoApi: ReactiveMongoApi) {

  protected def todoCollection: Future[BSONCollection] = mongoApi.database.map(_.collection("todo_items"))

  protected def todoJSON: Future[JSONCollection] = mongoApi.database.map(_.collection("todo_items"))

  /**
    *
    * @param username The username for which to add the goal
    * @param item
    * @return
    */
  def addTodoItem(username: String, item: TodoItem): Future[ResultInfo[String]] = {

    todoCollection.flatMap(_.insert(item)).map(
      result =>
        if (result.ok) ResultInfo.succeedWithMessage("added item to list")
        else ResultInfo.failWithMessage("failed to add item")
    )
  }

  /**
    *
    * @param username
    * @param id
    * @return
    */
  def deleteTodoItem(username: String, id: BSONObjectID): Future[ResultInfo[String]] = {

    todoCollection.flatMap(_.remove(usernameAndID(username, id), firstMatchOnly = true)).map(
      result =>
        if (result.ok) ResultInfo.succeedWithMessage("removed item from list")
        else ResultInfo.failWithMessage("failed to delete item")
    )
  }

  /**
    * Mark a todo item as completed
    *
    * @param username The username
    * @param id
    * @return
    */
  def completeTodoItem(username: String, id: String, coords: Point): Future[ResultInfo[String]] = {

    val s = BSONDocument(
      "username" -> username,
      "_id" -> BSONObjectID(id)
    )

    val u = BSONDocument(
      "$currentDate" -> BSONDocument(
        "endTime" -> true
      ),
      "$set" -> BSONDocument(
        "endPos" -> coords
      )
    )

    todoCollection.flatMap(_.update(s, u)).map(result =>
      if (result.ok) ResultInfo.succeedWithMessage(s"completed item: ${result.n}")
      else ResultInfo.failWithMessage("failed to add item")
    )
  }

  /**
    *
    * @param username
    * @return
    */
  def getTodoItems(username: String): Future[ResultInfo[List[JsObject]]] = {

    val s = JsObject(Map(
      "username" -> JsString(username)
    ))

    todoJSON.flatMap(
      _.find(s).cursor[JsObject]().collect[List]().map(
        itemList => ResultInfo.success(s"Retrieved todo items for $username", itemList)
      )
    )
  }

}
