package models

// Standard Library
import scala.concurrent.Future

// Project
import constructs.{JournalEntry, ResultInfo}

// Play Framework
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// Reactive Mongo
import reactivemongo.play.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import play.modules.reactivemongo.json.collection.JSONCollection

/**
  * Model layer to manage journal entries
  *
  * @param mongoApi Holds a reference to the database
  */
class JournalEntries(protected val mongoApi: ReactiveMongoApi) {

  // An interface to the journal_entries collection as BSON
  protected def bsonJournalEntriesCollection: Future[BSONCollection] = mongoApi.database.map(_.collection[BSONCollection]("journal_entries"))

  /**
    * Record a journal entry in the database
    *
    * @param username The username submitting the journal entry
    * @param text     The journal entry to be recorded
    * @return
    */
  def addJournalEntry(username: String, text: String): Future[ResultInfo] = {

    bsonJournalEntriesCollection.flatMap(_.insert(JournalEntry(username, text, System.currentTimeMillis())).map(result =>
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
  def journalEntriesForUsername(username: String): Future[List[JsObject]] = {

    def c: JSONCollection = mongoApi.db.collection[JSONCollection]("journal_entries")

    val selector = Json.obj("username" -> username)

    val projector = Json.obj("_id" -> 0)

    c.find(selector, projector).cursor[JsObject]().collect[List]()
  }
}
