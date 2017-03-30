package forms

import play.api.data.Form
import play.api.data.Forms._


case class AddJournalEntryForm(username: String, password: String, text: String, timestamp: Long)


object AddJournalEntryForm {

  val form: Form[AddJournalEntryForm] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "text" -> nonEmptyText,
      "timestamp" -> longNumber
    )(AddJournalEntryForm.apply)(AddJournalEntryForm.unapply)
  )

}