package forms

import play.api.data.Form
import play.api.data.Forms._


case class PasswordAndUsername(username: String, password: String)

object PasswordAndUsername {

  val form: Form[PasswordAndUsername] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(PasswordAndUsername.apply)(PasswordAndUsername.unapply)
  )
}
