package forms

import play.api.data.Form
import play.api.data.Forms._


case class RenameSubjectForm(user_id: Int, password: String, oldName: String, newName: String)

object RenameSubjectForm {

  val form: Form[RenameSubjectForm] = Form(
    mapping(
      "user_id" -> number,
      "password" -> nonEmptyText,
      "oldName" -> nonEmptyText,
      "newName" -> nonEmptyText
    )(RenameSubjectForm.apply)(RenameSubjectForm.unapply)
  )
}
