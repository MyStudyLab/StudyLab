package forms

import play.api.data.Form
import play.api.data.Forms._


case class RemoveSubjectForm(subject: String)

object RemoveSubjectForm {

  val form: Form[RemoveSubjectForm] = Form(
    mapping(
      "subject" -> nonEmptyText
    )(RemoveSubjectForm.apply)(RemoveSubjectForm.unapply)
  )
}
