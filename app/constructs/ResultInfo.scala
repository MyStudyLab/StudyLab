package constructs

case class ResultInfo(success: Boolean, message: String)

object ResultInfo {

  val badUsernameOrPass = ResultInfo(success = false, "Incorrect username or password")

  val databaseError = ResultInfo(success = false, "Database error")

  def failWithMessage(message: String) = ResultInfo(success = false, message)

  def succeedWithMessage(message: String) = ResultInfo(success = true, message)

}
