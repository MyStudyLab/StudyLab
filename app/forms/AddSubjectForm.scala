package forms

import play.api.data.Form
import play.api.data.Forms._


case class AddSubjectForm(user_id: Int, password: String, subject: String)

object AddSubjectForm {

  val form: Form[AddSubjectForm] = Form(
    mapping(
      "user_id" -> number,
      "password" -> nonEmptyText,
      "subject" -> nonEmptyText
    )(AddSubjectForm.apply)(AddSubjectForm.unapply)
  )
}
