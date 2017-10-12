package forms

import play.api.data.Form
import play.api.data.Forms._

case class DeleteTodoItemForm(id: String)

object DeleteTodoItemForm {

  val form: Form[DeleteTodoItemForm] = Form(
    mapping(
      "id" -> text(maxLength = 24)
    )(DeleteTodoItemForm.apply)(DeleteTodoItemForm.unapply)
  )

}
