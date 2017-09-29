package forms

import play.api.data.Form
import play.api.data.Forms._

case class SessionStopForm(message: String)

object SessionStopForm {

  val stopForm: Form[SessionStopForm] = Form(
    mapping(
      "message" -> text
    )(SessionStopForm.apply)(SessionStopForm.unapply)
  )
}