package controllers

// Standard Library
import javax.inject.Inject

// Play Framework
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// Reactive Mongo
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

/**
  * Controller that handles requests seeking quote data
  *
  * @param reactiveMongoApi Reference to the Reactive Mongo API
  */
class Quotes @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {

  /**
    * An instance of the quotes model
    */
  protected val quotes = new models.Quotes(reactiveMongoApi)

  /**
    *
    *
    * @param username The username for which to retrieve quotes.
    * @return
    */
  def getQuotes(username: String) = Action.async {

    quotes.getQuotes(username).map(optJson => Ok(optJson.getOrElse(Json.obj()).value("quotes")))
  }

}
