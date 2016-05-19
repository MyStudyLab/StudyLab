package forms

import play.api.data.Form
import play.api.data.Forms._

case class SessionStartForm(user_id: Int, password: String, subject: String)

object SessionStartForm {

  val startForm: Form[SessionStartForm] = Form(
    mapping(
      "user_id" -> number,
      "password" -> nonEmptyText,
      "subject" -> nonEmptyText
    )(SessionStartForm.apply)(SessionStartForm.unapply)
  )
}