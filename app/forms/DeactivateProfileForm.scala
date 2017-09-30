package forms

import play.api.data.Form
import play.api.data.Forms._

case class DeactivateProfileForm(password: String) {

}

object DeactivateProfileForm {

  val form: Form[DeactivateProfileForm] = Form(
    mapping(
      "password" -> nonEmptyText
    )(DeactivateProfileForm.apply)(DeactivateProfileForm.unapply)
  )

}