package forms

import play.api.data.Form
import play.api.data.Forms._

case class DeleteJournalEntryForm(id: String)

object DeleteJournalEntryForm {

  val form: Form[DeleteJournalEntryForm] = Form(
    mapping(
      "id" -> nonEmptyText(maxLength = 24)
    )(DeleteJournalEntryForm.apply)(DeleteJournalEntryForm.unapply)
  )

}