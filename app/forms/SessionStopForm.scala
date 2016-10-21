package forms

import play.api.data.Form
import play.api.data.Forms._

case class SessionStopForm(username: String, password: String, message: String)

object SessionStopForm {

  val stopForm: Form[SessionStopForm] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "message" -> text
    )(SessionStopForm.apply)(SessionStopForm.unapply)
  )
}