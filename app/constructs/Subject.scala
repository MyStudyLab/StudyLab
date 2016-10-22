package constructs

import play.api.libs.json.Json


/**
  * Represents a subject that can be studied.
  *
  * TODO: Remove the isLanguage field
  *
  * @param name        The name of the subject.
  * @param added       The time when the subject was added.
  * @param description A description of this subject.
  */
case class Subject(name: String, added: Long, description: String)

object Subject {

  import reactivemongo.bson.Macros

  // Implicitly convert to/from BSON
  implicit val SubjectHandler = Macros.handler[Subject]

  // Implicitly convert to JSON
  implicit val SubjectWrites = Json.writes[Subject]
}