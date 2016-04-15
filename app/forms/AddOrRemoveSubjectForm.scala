package forms

import play.api.data.Form
import play.api.data.Forms._


case class AddOrRemoveSubjectForm(user_id: Int, password: String, subject: String)

object AddOrRemoveSubjectForm {

  val form: Form[AddOrRemoveSubjectForm] = Form(
    mapping(
      "user_id" -> number,
      "password" -> nonEmptyText,
      "subject" -> nonEmptyText
    )(AddOrRemoveSubjectForm.apply)(AddOrRemoveSubjectForm.unapply)
  )
}
