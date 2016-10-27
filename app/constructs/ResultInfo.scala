package constructs

import play.api.libs.json.Json

// TODO: How to add a payload field? Type parameter with implicit Json.Writer?
case class ResultInfo(success: Boolean, message: String, timestamp: Long)

object ResultInfo {

  // Implicitly convert to JSON
  implicit val ResultInfoWriter = Json.writes[ResultInfo]

  // All of the following helpers are functions so that the timestamp will be generated at the proper time

  /**
    * Indicate a failed operation with the given message.
    *
    * @param message A message describing the result
    * @return
    */
  def failWithMessage(message: String) = ResultInfo(success = false, message, System.currentTimeMillis())


  /**
    * Indicate a successful operation with the given message
    *
    * @param message A message describing the result
    * @return
    */
  def succeedWithMessage(message: String) = ResultInfo(success = true, message, System.currentTimeMillis())


  /**
    * A result indicating that invalid credentials were provided
    *
    * @return
    */
  def badUsernameOrPass = failWithMessage("Incorrect username or password")

  /**
    *
    * @return
    */
  def invalidUsername = failWithMessage("Invalid Username")

  /**
    * A result indicating that the user was already studying.
    *
    * @return
    */
  def alreadyStudying = failWithMessage("Already studying")


  /**
    * A result indicating that the user was not studying.
    *
    * @return
    */
  def notStudying = failWithMessage("Not studying")


  /**
    * A result indicating that the given subject was invalid.
    *
    * @return
    */
  def invalidSubject = failWithMessage("Invalid subject")

  /**
    *
    * @return
    */
  def invalidForm = failWithMessage("Invalid form")
}
