
// Standard Library
import scala.concurrent.Future

// Project
import constructs.ResultInfo

// Play Framework
import play.api.libs.json.Json
import play.api.mvc.{Request, Result}
import play.api.mvc.Results.{Ok, Redirect}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
  * Package object for the controllers package. Contains controller-specific helpers.
  */
package object controllers {

  // Response indicating the request form was invalid.
  def invalidFormResponse = Future(Ok(ResultInfo.invalidForm.toJson))

  /**
    * Get the username from the session cookie
    *
    * @param func    The function producing a future result
    * @param request The request being processed
    * @tparam A
    * @return
    */
  def withUsername[A](func: String => Future[Result])(implicit request: Request[A]): Future[Result] = {

    request.session.get("connected").fold(Future(Redirect(routes.Application.loginPage())))(username => func(username))
  }

  def withUsername[A](func: String => Result)(implicit request: Request[A]): Result = {

    request.session.get("connected").fold(Redirect(routes.Application.loginPage()))(username => func(username))
  }

  /**
    * Remove excess whitespace from the given text
    *
    * TODO: Replace 2+ newlines with a single newline
    *
    * @param text The raw input text
    * @return
    */
  def withoutExcessWhitespace(text: String): String = {
    "\\s+".r.replaceAllIn(text.trim(), " ")
  }

}
