package controllers

// Standard Library
import javax.inject.Inject

import constructs.{CumulativeGoal, Point, TodoItem}

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
class Todo @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {

  protected val todo = new models.Todo(reactiveMongoApi)

  /**
    * Add a todo item for the given username
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
    *
    * @return
    */
  def getTodoItems = Action.async { implicit request =>

    withUsername(username =>
      todo.getTodoItems(username).map(resInfo => Ok(resInfo.toJson))
    )
  }
}
