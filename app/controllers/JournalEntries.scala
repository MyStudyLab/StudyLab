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
  def addJournalEntry = Action.async { implicit request =>

    withUsername(username => {
      AddJournalEntryForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        goodForm => {

          val cleanedEntry = withoutExcessWhitespace(goodForm.text)

          journalEntries.addJournalEntry(username, cleanedEntry).map(resultInfo => Ok(Json.toJson(resultInfo)))
        }
      )
    })

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
}
