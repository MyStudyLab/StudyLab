package forms

import play.api.data.Form
import play.api.data.Forms._


case class MergeSubjectsForm(username: String, password: String, absorbed: String, absorbing: String)

object MergeSubjectsForm {

  val form: Form[MergeSubjectsForm] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "absorbed" -> nonEmptyText,
      "absorbing" -> nonEmptyText
    )(MergeSubjectsForm.apply)(MergeSubjectsForm.unapply)
  )

}
