package helpers

case class ResultInfo(success: Boolean, message: String)

object ResultInfo {

  val badUsernameOrPass = ResultInfo(false, "Incorrect username or password")

  val databaseError = ResultInfo(false, "Database error")

}
