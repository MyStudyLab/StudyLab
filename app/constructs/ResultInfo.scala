package constructs

case class ResultInfo(success: Boolean, message: String)

object ResultInfo {

  def failWithMessage(message: String) = ResultInfo(success = false, message)

  def succeedWithMessage(message: String) = ResultInfo(success = true, message)

  val badUsernameOrPass = failWithMessage("Incorrect username or password")

  val databaseError = failWithMessage("Database error")

  // A result indicating that the user was already studying.
  val alreadyStudying = failWithMessage("Already studying")

  // A result indicating that the user was not studying.
  val notStudying = failWithMessage("Not studying")

  // A result indicating that the given subject was invalid.
  val invalidSubject = failWithMessage("Invalid subject")
}
