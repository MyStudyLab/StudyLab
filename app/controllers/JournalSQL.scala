package controllers

// Standard Library
import javax.inject.Inject

// Play Framework
import play.api.db.Database
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class JournalSQL @Inject()(db: Database) extends Controller {


  val model = new models.JournalSQL(db)

  def add = Action.async { implicit request =>

    withUsername(username => {

      forms.AddJournalEntryForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        goodForm => {

          model.add(username, goodForm.text, (goodForm.latitude, goodForm.longitude)).map(status => Ok(""))

        }
      )

    })

  }

}
