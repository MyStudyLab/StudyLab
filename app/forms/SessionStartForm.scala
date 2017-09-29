package forms

import play.api.data.Form
import play.api.data.Forms._

case class SessionStartForm(subject: String)

object SessionStartForm {

  val startForm: Form[SessionStartForm] = Form(
    mapping(
      "subject" -> nonEmptyText
    )(SessionStartForm.apply)(SessionStartForm.unapply)
  )
}