package forms

import play.api.data.Form
import play.api.data.Forms._

case class SetJournalPublicityForm(id: String, public: Boolean)

object SetJournalPublicityForm {

  val form: Form[SetJournalPublicityForm] = Form(
    mapping(
      "id" -> nonEmptyText(maxLength = 24),
      "public" -> boolean
    )(SetJournalPublicityForm.apply)(SetJournalPublicityForm.unapply)
  )

}