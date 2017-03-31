package controllers

// Standard Library
import javax.inject.Inject

// Project
import forms.AddJournalEntryForm

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

  // Reference to the Journal Entry model
  protected val journalEntries = new models.JournalEntries(reactiveMongoApi)


  /**
    * Invoke the model layer to record a new journal entry.
    *
    * @return
    */
  protected def add = Action.async { implicit request =>

    AddJournalEntryForm.form.bindFromRequest()(request).fold(
      badForm => invalidFormResponse,
      goodForm => journalEntries.addJournalEntry(goodForm.username, goodForm.text).map(resultInfo => Ok(Json.toJson(resultInfo)))
    )
  }

  /**
    * Get all journal entries for the given username
    *
    * @param username The username for which to retrieve data
    * @return
    */
  def journalEntriesForUsername(username: String) = Action.async { implicit request =>

    journalEntries.journalEntriesForUsername(username).map(entryList => Ok(Json.toJson(entryList)))

  }

  /**
    * Add a journal entry after checking user credentials
    *
    * @return
    */
  def addJournalEntry() = checked(add)(reactiveMongoApi)
}
