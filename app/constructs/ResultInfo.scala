package constructs

// Play Framework
import play.api.libs.json.{Json, Writes}

// TODO: How to add a payload field? Type parameter with implicit Json.Writer?
/**
  * Defines the structure of responses to API calls
  *
  * @param success   Indicates whether the operation succeeded
  * @param message   A message regarding the success/failure of the operation
  * @param timestamp A timestamp of the response
  */
case class ResultInfo(success: Boolean, message: String, timestamp: Long)

object ResultInfo {

  // Implicitly convert to JSON
  implicit val ResultInfoWriter: Writes[ResultInfo] = Json.writes[ResultInfo]

  // All of the following helpers are functions so that the timestamp will be generated at the proper time

  /**
    * Indicate a failed operation with the given message.
    *
    * @param message A message describing the result
    * @return
    */
  def failWithMessage(message: String): ResultInfo = ResultInfo(success = false, message, System.currentTimeMillis())


  /**
    * Indicate a successful operation with the given message
    *
    * @param message A message describing the result
    * @return
    */
  def succeedWithMessage(message: String): ResultInfo = ResultInfo(success = true, message, System.currentTimeMillis())


  /**
    * A result indicating that invalid credentials were provided
    *
    * @return
    */
  def badUsernameOrPass: ResultInfo = failWithMessage("Incorrect username or password")

  /**
    *
    * @return
    */
  def invalidUsername: ResultInfo = failWithMessage("Invalid Username")

  /**
    * A result indicating that the user was already studying.
    *
    * @return
    */
  def alreadyStudying: ResultInfo = failWithMessage("Already studying")


  /**
    * A result indicating that the user was not studying.
    *
    * @return
    */
  def notStudying: ResultInfo = failWithMessage("Not studying")


  /**
    * A result indicating that the given subject was invalid.
    *
    * @return
    */
  def invalidSubject: ResultInfo = failWithMessage("Invalid subject")

  /**
    *
    * @return
    */
  def invalidForm: ResultInfo = failWithMessage("Invalid form")
}
