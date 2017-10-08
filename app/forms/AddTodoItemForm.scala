package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

case class AddTodoItemForm(text: String, latitude: Double, longitude: Double)

object AddTodoItemForm {

  val form: Form[AddTodoItemForm] = Form(
    mapping(
      "text" -> nonEmptyText,
      "latitude" -> of[Double],
      "longitude" -> of[Double]
    )(AddTodoItemForm.apply)(AddTodoItemForm.unapply)
  )

}
