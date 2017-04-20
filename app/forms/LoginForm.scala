package forms

import play.api.data.Form
import play.api.data.Forms._


/**
  * Form used during user login
  *
  * @param username
  * @param password
  */
case class LoginForm(username: String, password: String) {

}

object LoginForm {

  val form: Form[LoginForm] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginForm.apply)(LoginForm.unapply)
  )

}