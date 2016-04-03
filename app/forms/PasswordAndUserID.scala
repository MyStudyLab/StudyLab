package forms

import play.api.data.Form
import play.api.data.Forms._


case class PasswordAndUserID(user_id: Int, password: String)

object PasswordAndUserID {

  val form: Form[PasswordAndUserID] = Form(
    mapping(
      "user_id" -> number,
      "password" -> nonEmptyText
    )(PasswordAndUserID.apply)(PasswordAndUserID.unapply)
  )
}
