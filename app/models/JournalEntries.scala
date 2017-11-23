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
  protected def bsonCollection: Future[BSONCollection] = mongoApi.database.map(_.collection[BSONCollection]("journal_entries"))

  protected def jsonCollection: Future[JSONCollection] = mongoApi.database.map(_.collection[JSONCollection]("journal_entries"))

  /**
    * Add a journal entry to the database
    *
    * @param entry The journal entry to be added
    * @return
    */
  def add(entry: JournalEntry): Future[ResultInfo[JsValue]] = {

    bsonCollection.flatMap(_.insert(entry).map(result =>
      if (result.ok) ResultInfo.success("Journal entry recorded", Json.toJson(entry))
      else ResultInfo.failure("entry not recorded", Json.obj()))
    )
  }


  /**
    *
    * @param entry
    * @return
    */
  def addJournalEntryWithId(entry: JournalEntryWithID): Future[ResultInfo[JsValue]] = {

    bsonCollection.flatMap(_.insert(entry).map(result =>
      if (result.ok) ResultInfo.success("Journal entry recorded", Json.toJson(entry))
      else ResultInfo.failure("Entry not recorded", Json.obj()))
    )
  }


  /**
    * Delete a journal entry
    *
    * @param username The author of the entry
    * @param id       The object ID of the entry
    * @return
    */
  def delete(username: String, id: BSONObjectID): Future[ResultInfo[String]] = {

    bsonCollection.flatMap(_.remove(usernameAndID(username, id), firstMatchOnly = true).map(result =>
      if (result.ok) ResultInfo.success("Journal entry deleted", id.stringify)
      else ResultInfo.failWithMessage("Entry not deleted")
    ))
  }


  /**
    * Set the publicity of a journal entry
    *
    * @param username The author of the entry
    * @param id       The object ID of the entry
    * @param public   The publicity
    * @return
    */
  def setPublicity(username: String, id: BSONObjectID, public: Boolean): Future[ResultInfo[String]] = {

    val u = BSONDocument(
      "$set" -> BSONDocument(
        "public" -> public
      )
    )

    bsonCollection.flatMap(_.update(usernameAndID(username, id), u).map(result =>
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
  def getAllEntries(username: String): Future[ResultInfo[List[JsObject]]] = {

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
  def getPublicEntries(username: String): Future[ResultInfo[List[JsObject]]] = {

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
}
