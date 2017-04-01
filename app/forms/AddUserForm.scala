package forms

import play.api.data.Form
import play.api.data.Forms._

/**
  *
  * @param username
  * @param email
  * @param password
  */
case class AddUserForm(username: String, email: String, password: String)

object AddUserForm {

  val form: Form[AddUserForm] = Form(
    mapping(
      "username" -> nonEmptyText,
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )(AddUserForm.apply)(AddUserForm.unapply)
  )

}
