package controllers

// Standard Library
import javax.inject.Inject

import constructs.{JournalEntry, Point}

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

          val cleanedEntry = withoutExcessWhitespace(goodForm.entry)

          val position = Point(goodForm.longitude, goodForm.latitude)

          val entry = JournalEntry(username, cleanedEntry, System.currentTimeMillis(), position)

          journalEntries.addJournalEntry(entry).map(resultInfo => Ok(resultInfo.toJson))
        }
      )
    })

  }

  /**
    * Get all journal entries for the given username
    *
    * @return
    */
  def getJournalEntries = Action.async { implicit request =>

    withUsername(username =>
      journalEntries.journalEntriesForUsername(username).map(resInfo => Ok(resInfo.toJson))
    )


  }
}
