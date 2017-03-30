package models

// Standard Library
import scala.concurrent.Future

// Project
import constructs.{JournalEntry, ResultInfo}

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
    * @param entry The journal entry to be recorded
    * @return
    */
  def addJournalEntry(entry: JournalEntry): Future[ResultInfo] = {

    bsonJournalEntriesCollection.insert(entry).map(result =>
      if (result.ok) ResultInfo.succeedWithMessage("Journal entry recorded")
      else ResultInfo.failWithMessage(result.errmsg.getOrElse(ResultInfo.noErrMsg)))
  }
}
