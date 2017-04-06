package forms

import play.api.data.Form
import play.api.data.Forms._


case class RemoveSubjectForm(username: String, password: String, subject: String)

object RemoveSubjectForm {

  val form: Form[RemoveSubjectForm] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "subject" -> nonEmptyText
    )(RemoveSubjectForm.apply)(RemoveSubjectForm.unapply)
  )
}
