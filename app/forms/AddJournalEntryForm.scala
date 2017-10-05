package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._


case class AddJournalEntryForm(entry: String, latitude: Double, longitude: Double)


object AddJournalEntryForm {

  val form: Form[AddJournalEntryForm] = Form(
    mapping(
      "entry" -> nonEmptyText,
      "latitude" -> of[Double],
      "longitude" -> of[Double]
    )(AddJournalEntryForm.apply)(AddJournalEntryForm.unapply)
  )

}