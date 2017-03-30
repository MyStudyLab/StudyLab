package controllers

// Standard Library
import javax.inject.Inject
import scala.concurrent.Future

// Project
import forms.AddJournalEntryForm
import constructs.{JournalEntry, ResultInfo}

// Play Framework
import play.api.mvc.{Action, Controller}
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// Reactive Mongo
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}


/**
  *
  * @param reactiveMongoApi
  */
class JournalEntries @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {

  // Reference to the sessions model
  protected val journalEntries = new models.JournalEntries(reactiveMongoApi)

  // TODO: Put this method in a helper object or something
  // Response indicating the request form was invalid.
  protected def invalidFormResponse = Future(Ok(Json.toJson(ResultInfo.invalidForm)))


  /**
    * Invoke the model layer to record a new journal entry.
    *
    * @return
    */
  protected def addJournalEntry = Action.async { implicit request =>

    AddJournalEntryForm.form.bindFromRequest()(request).fold(
      badForm => invalidFormResponse,
      goodForm => journalEntries.addJournalEntry(goodForm.username, goodForm.text).map(resultInfo => Ok(Json.toJson(resultInfo)))
    )
  }

}
