package forms

import play.api.data.Form
import play.api.data.Forms._


case class AddMovieForm(user_id: Int, password: String, imdbID: String)

object AddMovieForm {

  val form: Form[AddMovieForm] = Form(
    mapping(
      "user_id" -> number,
      "password" -> nonEmptyText,
      "imdbID" -> nonEmptyText
    )(AddMovieForm.apply)(AddMovieForm.unapply)
  )
}
