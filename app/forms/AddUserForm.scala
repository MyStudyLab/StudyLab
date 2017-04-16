package forms

import play.api.data.Form
import play.api.data.Forms._

/**
  *
  * @param username
  * @param firstName
  * @param lastName
  * @param email
  * @param password
  */
case class AddUserForm(username: String, firstName: String, lastName: String, email: String, password: String)

object AddUserForm {

  val form: Form[AddUserForm] = Form(
    mapping(
      "username" -> nonEmptyText,
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )(AddUserForm.apply)(AddUserForm.unapply)
  )

}
