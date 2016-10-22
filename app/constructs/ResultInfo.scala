package constructs

case class ResultInfo(success: Boolean, message: String, timestamp: Long)

object ResultInfo {

  def failWithMessage(message: String) = ResultInfo(success = false, message, System.currentTimeMillis())

  def succeedWithMessage(message: String) = ResultInfo(success = true, message, System.currentTimeMillis())

  def badUsernameOrPass = failWithMessage("Incorrect username or password")

  def databaseError = failWithMessage("Database error")

  // A result indicating that the user was already studying.
  def alreadyStudying = failWithMessage("Already studying")

  // A result indicating that the user was not studying.
  def notStudying = failWithMessage("Not studying")

  // A result indicating that the given subject was invalid.
  def invalidSubject = failWithMessage("Invalid subject")
}
