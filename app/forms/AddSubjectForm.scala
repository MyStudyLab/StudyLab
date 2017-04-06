package forms

import play.api.data.Form
import play.api.data.Forms._


case class AddSubjectForm(username: String, password: String, subject: String, description: String)

object AddSubjectForm {

  val form: Form[AddSubjectForm] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "subject" -> nonEmptyText,
      "description" -> nonEmptyText
    )(AddSubjectForm.apply)(AddSubjectForm.unapply)
  )
}
