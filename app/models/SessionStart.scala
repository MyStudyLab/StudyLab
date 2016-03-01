package models

import play.api.data.Form
import play.api.data.Forms._

case class SessionStart(subject: String)

object SessionStart {

  val startForm: Form[SessionStart] = Form(
    mapping(
      "subject" -> nonEmptyText
    )(SessionStart.apply)(SessionStart.unapply)
  )
}