package forms

import play.api.data.Form
import play.api.data.Forms._


case class AddOrRemoveSubjectForm(username: String, password: String, subject: String, description: String)

object AddOrRemoveSubjectForm {

  val form: Form[AddOrRemoveSubjectForm] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "subject" -> nonEmptyText,
      "description" -> nonEmptyText
    )(AddOrRemoveSubjectForm.apply)(AddOrRemoveSubjectForm.unapply)
  )
}
