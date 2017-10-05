package forms

import play.api.data.Form
import play.api.data.Forms._

case class UpdateEmailForm(email: String)

object UpdateEmailForm {

  val form: Form[UpdateEmailForm] = Form(
    mapping(
      "email" -> nonEmptyText
    )(UpdateEmailForm.apply)(UpdateEmailForm.unapply)
  )

}