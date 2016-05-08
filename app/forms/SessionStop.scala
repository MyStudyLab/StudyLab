package forms

import play.api.data.Form
import play.api.data.Forms._

case class SessionStop(user_id: Int, password: String, message: String)

object SessionStop {

  val stopForm: Form[SessionStop] = Form(
    mapping(
      "user_id" -> number,
      "password" -> nonEmptyText,
      "message" -> text
    )(SessionStop.apply)(SessionStop.unapply)
  )
}