package forms

import play.api.data.Form
import play.api.data.Forms._

// TODO: Should these go in a forms package?
case class SessionStart(user_id: Int, subject: String)

object SessionStart {

  val startForm: Form[SessionStart] = Form(
    mapping(
      "user_id" -> number,
      "subject" -> nonEmptyText
    )(SessionStart.apply)(SessionStart.unapply)
  )
}