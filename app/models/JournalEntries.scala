package models

// Standard Library
import constructs.JournalEntryWithID
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future

// Project
import constructs.{JournalEntry, ResultInfo}
import helpers.Selectors.usernameAndID

// Play Framework
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// Reactive Mongo
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import play.modules.reactivemongo.json._

/**
  * Model layer to manage journal entries
  *
  * @param mongoApi Holds a reference to the database
  */
class JournalEntries(protected val mongoApi: ReactiveMongoApi) {

  // An interface to the journal_entries collection as BSON
  protected def journalCollection: Future[BSONCollection] = mongoApi.database.map(_.collection[BSONCollection]("journal_entries"))

  protected def jsonCollection: Future[JSONCollection] = mongoApi.database.map(_.collection[JSONCollection]("journal_entries"))

  /**
    * Record a journal entry in the database
    *
    * @param entry The journal entry to record
    * @return
    */
  def addJournalEntry(entry: JournalEntry): Future[ResultInfo[JsValue]] = {

    journalCollection.flatMap(_.insert(entry).map(result =>
      if (result.ok) ResultInfo.success("Journal entry recorded", entry.toGeoJson)
      else ResultInfo.failure("entry not recorded", Json.obj()))
    )
  }


  /**
    *
    * @param entry
    * @return
    */
  def addJournalEntryWithId(entry: JournalEntryWithID): Future[ResultInfo[JsValue]] = {

    journalCollection.flatMap(_.insert(entry).map(result =>
      if (result.ok) ResultInfo.success("Journal entry recorded", Json.toJson(entry))
      else ResultInfo.failure("Entry not recorded", Json.obj()))
    )
  }


  /**
    * Delete a journal entry
    *
    * @param username
    * @param id
    * @return
    */
  def delete(username: String, id: BSONObjectID): Future[ResultInfo[String]] = {

    journalCollection.flatMap(_.remove(usernameAndID(username, id), firstMatchOnly = true).map(result =>
      if (result.ok) ResultInfo.success("Journal entry deleted", id.stringify)
      else ResultInfo.failWithMessage("Entry not deleted")
    ))

  }


  /**
    * Set the publicity of a journal entry
    *
    * @return
    */
  def setPublicity(username: String, id: BSONObjectID, public: Boolean): Future[ResultInfo[String]] = {

    val u = BSONDocument(
      "$set" -> BSONDocument(
        "public" -> public
      )
    )

    journalCollection.flatMap(_.update(usernameAndID(username, id), u).map(result =>
      if (result.ok) ResultInfo.succeedWithMessage("Journal entry recorded")
      else ResultInfo.failWithMessage("entry not recorded")
    ))
  }


  /**
    * Get all of the journal entries for the given username
    *
    * @param username The username for which to retrieve data
    * @return
    */
  def journalEntriesForUsername(username: String): Future[ResultInfo[List[JsObject]]] = {

    val s = Json.obj(
      "username" -> username
    )

    jsonCollection.flatMap(
      _.find(s).cursor[JsObject]().collect[List]().map(
        entries => ResultInfo.success(s"retrieved journal entries for $username", entries)
      )
    )
  }


  /**
    * Get only the public entries for the given username
    *
    * @param username The username for which to retrieve data
    * @return
    */
  def publicEntries(username: String): Future[ResultInfo[List[JsObject]]] = {

    val s = Json.obj(
      "username" -> username,
      "public" -> true
    )

    // TODO: how to check if the user exists (as opposed to just having no entries)?

    jsonCollection.flatMap(
      _.find(s).cursor[JsObject]().collect[List]().map(
        entries => ResultInfo.success(s"Retrieved public entries for $username", entries)
      )
    )
  }


  def journalEntriesWithID(username: String): Future[ResultInfo[List[JournalEntry]]] = {
    ???
  }
}
