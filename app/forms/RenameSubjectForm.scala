package forms

import play.api.data.Form
import play.api.data.Forms._


case class RenameSubjectForm(oldName: String, newName: String)

object RenameSubjectForm {

  val form: Form[RenameSubjectForm] = Form(
    mapping(
      "oldName" -> nonEmptyText,
      "newName" -> nonEmptyText
    )(RenameSubjectForm.apply)(RenameSubjectForm.unapply)
  )
}
