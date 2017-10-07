package constructs

// Play Framework
import play.api.libs.json._

// TODO: How to add a payload field? Type parameter with implicit Json.Writer?
/**
  * Defines the structure of responses to API calls
  *
  * @param success   Indicates whether the operation succeeded
  * @param message   A message regarding the success/failure of the operation
  * @param timestamp When the response was created by the server
  */
case class ResultInfo[T](success: Boolean, message: String, timestamp: Long, payload: T)(implicit payloadWrites: Writes[T]) {

  def toJson: JsValue = JsObject(Seq(
    "success" -> JsBoolean(success),
    "message" -> JsString(message),
    "timestamp" -> JsNumber(timestamp),
    "payload" -> payloadWrites.writes(payload)
  ))

}

object ResultInfo {

  def apply(success: Boolean, message: String): ResultInfo[String] = new ResultInfo(success, message, System.currentTimeMillis(), "")

  def apply(success: Boolean, message: String, timestamp: Long): ResultInfo[String] = new ResultInfo(success, message, timestamp, "")

  // Implicitly convert to JSON
  //implicit def ResultInfoWriter[T](implicit writes: Writes[T]): Writes[ResultInfo[T]] = Json.writes[ResultInfo[T]]

  // Message for when Mongo fails without providing one
  val noErrMsg = "Failed without error message"


  // TODO: See what version of the ReactiveMongo version fixes the bug with custom apply methods
  //def apply(success: Boolean, message: String): ResultInfo = ResultInfo(success, message, System.currentTimeMillis())

  // All of the following helpers are functions so that the timestamp will be generated at the proper time

  /**
    * Indicate a failed operation with the given message.
    *
    * @param message A message describing the result
    * @return
    */
  def failWithMessage(message: String): ResultInfo[String] = ResultInfo(success = false, message, System.currentTimeMillis())


  /**
    * Indicate a successful operation with the given message
    *
    * @param message A message describing the result
    * @return
    */
  def succeedWithMessage(message: String): ResultInfo[String] = ResultInfo(success = true, message, System.currentTimeMillis())


  /**
    * A result indicating that invalid credentials were provided
    *
    * @return
    */
  def badUsernameOrPass: ResultInfo[String] = failWithMessage("Incorrect username or password")

  /**
    *
    * @return
    */
  def invalidUsername: ResultInfo[String] = failWithMessage("Invalid Username")

  /**
    * A result indicating that the user was already studying.
    *
    * @return
    */
  def alreadyStudying: ResultInfo[String] = failWithMessage("Already studying")


  /**
    * A result indicating that the user was not studying.
    *
    * @return
    */
  def notStudying: ResultInfo[String] = failWithMessage("Not studying")


  /**
    * A result indicating that the given subject was invalid.
    *
    * @return
    */
  def invalidSubject: ResultInfo[String] = failWithMessage("Invalid subject")

  /**
    *
    * @return
    */
  def invalidForm: ResultInfo[String] = failWithMessage("Invalid form")
}
