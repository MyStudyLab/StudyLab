package forms

import play.api.data.Form
import play.api.data.Forms._


case class AddSubjectForm(subject: String, description: String)

object AddSubjectForm {

  val form: Form[AddSubjectForm] = Form(
    mapping(
      "subject" -> nonEmptyText,
      "description" -> nonEmptyText
    )(AddSubjectForm.apply)(AddSubjectForm.unapply)
  )
}
