package forms

import play.api.data.Form
import play.api.data.Forms._

case class SessionStopForm(user_id: Int, password: String, message: String)

object SessionStopForm {

  val stopForm: Form[SessionStopForm] = Form(
    mapping(
      "user_id" -> number,
      "password" -> nonEmptyText,
      "message" -> text
    )(SessionStopForm.apply)(SessionStopForm.unapply)
  )
}