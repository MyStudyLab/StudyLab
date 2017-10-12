package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

case class CompleteTodoItemForm(id: String, latitude: Double, longitude: Double)

object CompleteTodoItemForm {

  val form: Form[CompleteTodoItemForm] = Form(
    mapping(
      "id" -> text(maxLength = 24),
      "latitude" -> of[Double],
      "longitude" -> of[Double]
    )(CompleteTodoItemForm.apply)(CompleteTodoItemForm.unapply)
  )

}
