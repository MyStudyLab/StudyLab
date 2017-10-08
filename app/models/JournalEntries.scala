package models

// Standard Library
import scala.concurrent.Future

// Project
import constructs.{JournalEntry, ResultInfo}
import helpers.Selectors.usernameSelector

// Play Framework
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// Reactive Mongo
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection

/**
  * Model layer to manage journal entries
  *
  * @param mongoApi Holds a reference to the database
  */
class JournalEntries(protected val mongoApi: ReactiveMongoApi) {

  // An interface to the journal_entries collection as BSON
  protected def journalCollection: Future[BSONCollection] = mongoApi.database.map(_.collection[BSONCollection]("journal_entries"))

  /**
    * Record a journal entry in the database
    *
    * @param entry The journal entry to record
    * @return
    */
  def addJournalEntry(entry: JournalEntry): Future[ResultInfo[String]] = {

    journalCollection.flatMap(_.insert(entry).map(result =>
      if (result.ok) ResultInfo.succeedWithMessage("Journal entry recorded")
      else ResultInfo.failWithMessage("entry not recorded"))
    )
  }

  /**
    * Get all of the journal entries for the given username
    *
    * @param username The username for which to retrieve data
    * @return
    */
  def journalEntriesForUsername(username: String): Future[ResultInfo[List[JournalEntry]]] = {

    journalCollection.flatMap(
      _.find(usernameSelector(username)).cursor[JournalEntry]().collect[List]().map(
        entries => ResultInfo.success(s"retrieved journal entries for $username", entries)
      )
    )
  }
}
