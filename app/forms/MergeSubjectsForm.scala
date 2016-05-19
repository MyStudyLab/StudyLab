package forms

import play.api.data.Form
import play.api.data.Forms._


case class MergeSubjectsForm(user_id: Int, password: String, absorbed: String, absorbing: String)

object MergeSubjectsForm {

  val form: Form[MergeSubjectsForm] = Form(
    mapping(
      "user_id" -> number,
      "password" -> nonEmptyText,
      "absorbed" -> nonEmptyText,
      "absorbing" -> nonEmptyText
    )(MergeSubjectsForm.apply)(MergeSubjectsForm.unapply)
  )

}
