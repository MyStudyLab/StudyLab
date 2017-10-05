package forms

import play.api.data.Form
import play.api.data.Forms._

case class UpdatePasswordForm(password: String)

object UpdatePasswordForm {

  val form: Form[UpdatePasswordForm] = Form(
    mapping(
      "password" -> nonEmptyText
    )(UpdatePasswordForm.apply)(UpdatePasswordForm.unapply)
  )

}