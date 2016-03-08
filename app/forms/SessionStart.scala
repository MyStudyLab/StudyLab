package forms

import play.api.data.Form
import play.api.data.Forms._

case class SessionStart(user_id: Int, password: String, subject: String)

object SessionStart {

  val startForm: Form[SessionStart] = Form(
    mapping(
      "user_id" -> number,
      "password" -> nonEmptyText,
      "subject" -> nonEmptyText
    )(SessionStart.apply)(SessionStart.unapply)
  )
}