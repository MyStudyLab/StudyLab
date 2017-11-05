package constructs

// Reactive Mongo
import play.api.libs.json._
import reactivemongo.play.json._
import reactivemongo.bson.BSONObjectID


/**
  * Represents a journal entry
  *
  * @param username  The username responsible for the journal entry
  * @param text      The content of the journal entry
  * @param timestamp The time at which the journal entry was recorded
  * @param pos       The position where the journal entry was recorded
  */
case class JournalEntryWithID(_id: BSONObjectID, username: String, text: String, timestamp: Long, pos: Point, public: Boolean, sentiment: Double, inferredSubjects: List[String])

object JournalEntryWithID {

  // Helper to generate the object ID
  def apply(username: String, text: String, timestamp: Long, pos: Point, public: Boolean, sentiment: Double, inferredSubjects: List[String]): JournalEntryWithID = new JournalEntryWithID(BSONObjectID.generate(), username, text, timestamp, pos, public, sentiment, inferredSubjects)

  // Implicitly convert to JSON
  implicit val writes = Json.writes[JournalEntryWithID]

  import reactivemongo.bson.Macros

  // Implicitly convert to/from BSON
  implicit val handler = Macros.handler[JournalEntryWithID]

}