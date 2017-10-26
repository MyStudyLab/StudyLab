package controllers

// Standard Library
import javax.inject.Inject

import akka.actor.Status.Success
import constructs.{CumulativeGoal, Point, TodoItem}
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

// Project
import constructs.ResultInfo
import forms._

// Play Framework
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// Reactive Mongo
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}


/**
  * Controller to manage the study sessions API.
  *
  * @param reactiveMongoApi Holds a reference to the database.
  */
class Todo @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {

  protected val todo = new models.Todo(reactiveMongoApi)

  /**
    * Add a todo item for a username
    *
    * @return
    */
  def addTodoItem = Action.async { implicit request =>

    withUsername(username => {

      AddTodoItemForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        form => {

          val item = TodoItem(username, form.text, System.currentTimeMillis(), Point(form.longitude, form.latitude))

          todo.addTodoItem(username, item).map(result => Ok(result.toJson))
        }
      )

    })
  }

  /**
    * Delete a todo item for a username
    *
    * @return
    */
  def deleteTodoItem = Action.async { implicit request =>

    withUsername(username => {

      DeleteTodoItemForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        form => {

          BSONObjectID.parse(form.id).toOption.fold(
            Future(Ok(ResultInfo.failWithMessage("invalid object id").toJson))
          )(oid => todo.deleteTodoItem(username, oid).map(result => Ok(result.toJson)))
        }
      )
    })

  }

  /**
    * Complete a todo item for a username
    *
    * @return
    */
  def completeTodoItem = Action.async { implicit request =>

    withUsername(username => {

      CompleteTodoItemForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        form => {

          todo.completeTodoItem(username, form.id, Point(form.longitude, form.latitude)).map(
            result => Ok(result.toJson)
          )
        }
      )

    })
  }

  /**
    * Get all todo items for a username
    *
    * @return
    */
  def getTodoItems = Action.async { implicit request =>

    withUsername(username =>
      todo.getTodoItems(username).map(resInfo => Ok(resInfo.toJson))
    )
  }
}
