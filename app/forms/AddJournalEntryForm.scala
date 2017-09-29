package forms

import play.api.data.Form
import play.api.data.Forms._


case class AddJournalEntryForm(text: String)


object AddJournalEntryForm {

  val form: Form[AddJournalEntryForm] = Form(
    mapping(
      "text" -> nonEmptyText
    )(AddJournalEntryForm.apply)(AddJournalEntryForm.unapply)
  )

}