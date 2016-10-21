package constructs

/**
  * Used when both the user's status and subject list are needed.
  *
  * @param username The user's username.
  * @param status   The status of the user.
  * @param subjects The valid subjects for the user
  */
case class StatusSubjects(username: String, status: Status, subjects: Vector[Subject])


object StatusSubjects {

  import reactivemongo.bson.Macros

  // Implicitly converts to/from BSON
  implicit val StatusSubjectsHandler = Macros.handler[StatusSubjects]

}