package forms

import play.api.data.Form
import play.api.data.Forms._


case class AddOrRemoveSubjectForm(user_id: Int, password: String, subject: String, description: String)

object AddOrRemoveSubjectForm {

  val form: Form[AddOrRemoveSubjectForm] = Form(
    mapping(
      "user_id" -> number,
      "password" -> nonEmptyText,
      "subject" -> nonEmptyText,
      "description" -> nonEmptyText
    )(AddOrRemoveSubjectForm.apply)(AddOrRemoveSubjectForm.unapply)
  )
}
