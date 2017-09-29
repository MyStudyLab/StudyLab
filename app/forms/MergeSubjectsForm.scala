package forms

import play.api.data.Form
import play.api.data.Forms._


case class MergeSubjectsForm(absorbed: String, absorbing: String)

object MergeSubjectsForm {

  val form: Form[MergeSubjectsForm] = Form(
    mapping(
      "absorbed" -> nonEmptyText,
      "absorbing" -> nonEmptyText
    )(MergeSubjectsForm.apply)(MergeSubjectsForm.unapply)
  )

}
