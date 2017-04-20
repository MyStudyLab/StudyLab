
// Standard Library
import scala.concurrent.Future

// Project
import constructs.ResultInfo
import forms.PasswordAndUsername

// Play Framework
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Results.Ok
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// Reactive Mongo
import play.modules.reactivemongo.ReactiveMongoApi

/**
  * Package object for the controllers package. Contains controller-specific helpers.
  */
package object controllers {

  // Response indicating the request form was invalid.
  def invalidFormResponse = Future(Ok(Json.toJson(ResultInfo.invalidForm)))


  /**
    * Check a username and password before performing the given action.
    *
    * @param action The action to perform once the request is authenticated.
    * @tparam A Type parameter of the Action
    * @return
    */
  def checked[A](action: Action[A])(api: ReactiveMongoApi) = Action.async(action.parser) { implicit request =>

    // Reference to the users model
    val users = new models.Users(api)

    PasswordAndUsername.form.bindFromRequest()(request).fold(
      _ => invalidFormResponse,
      goodForm =>
        users.checkCredentials(goodForm.username, goodForm.password).flatMap(matched =>
          if (matched) action(request)
          else Future(Ok(Json.toJson(ResultInfo.badUsernameOrPass)))
        )
    )
  }

  /**
    *
    * @param action
    * @tparam A
    * @return
    */
  def checkSession[A](action: Action[A]) = Action.async(action.parser) { implicit request =>
    request.session.get("connected").fold(Future(Ok("")))(username => action(request))
  }
}
