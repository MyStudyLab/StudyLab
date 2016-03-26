package forms

import play.api.data.Form
import play.api.data.Forms._

case class Update(user_id: Int, password: String, collection: String)

object UpdateForm {

  val form: Form[Update] = Form(
    mapping(
      "user_id" -> number,
      "password" -> nonEmptyText,
      "collection" -> nonEmptyText
    )(Update.apply)(Update.unapply)
  )
}