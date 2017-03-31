package forms

import play.api.data.Form
import play.api.data.Forms._


case class AddJournalEntryForm(username: String, password: String, text: String)


object AddJournalEntryForm {

  val form: Form[AddJournalEntryForm] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "text" -> nonEmptyText
    )(AddJournalEntryForm.apply)(AddJournalEntryForm.unapply)
  )

}