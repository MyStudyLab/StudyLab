package forms

import play.api.data.Form
import play.api.data.Forms._

case class SessionCancelForm(username: String)

object SessionCancelForm {

  val form: Form[SessionCancelForm] = Form(
    mapping(
      "username" -> nonEmptyText
    )(SessionCancelForm.apply)(SessionCancelForm.unapply)
  )
}