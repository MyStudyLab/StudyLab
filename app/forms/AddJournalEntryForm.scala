package forms

import play.api.data.Form
import play.api.data.Forms._


case class AddJournalEntryForm(entry: String)


object AddJournalEntryForm {

  val form: Form[AddJournalEntryForm] = Form(
    mapping(
      "entry" -> nonEmptyText
    )(AddJournalEntryForm.apply)(AddJournalEntryForm.unapply)
  )

}