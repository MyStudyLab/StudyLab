package models

// Standard Library
import scala.concurrent.Future

// Project
import constructs.{JournalEntry, ResultInfo}

// Play Framework
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
  protected def bsonJournalEntriesCollection: BSONCollection = mongoApi.db.collection[BSONCollection]("journal_entries")


  /**
    * Record a journal entry in the database
    *
    * @param username The username submitting the journal entry
    * @param text     The journal entry to be recorded
    * @return
    */
  def addJournalEntry(username: String, text: String): Future[ResultInfo] = {

    bsonJournalEntriesCollection.insert(JournalEntry(username, text, System.currentTimeMillis())).map(result =>
      if (result.ok) ResultInfo.succeedWithMessage("Journal entry recorded")
      else ResultInfo.failWithMessage(result.errmsg.getOrElse(ResultInfo.noErrMsg)))
  }

  /**
    * Get all of the journal entries for the given username
    *
    * @param username The username for which to retrieve data
    * @return
    */
  def getJournalEntries(username: String): Future[ResultInfo] = {
    ???
  }
}
