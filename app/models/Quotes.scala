package models

// Standard Library
import scala.concurrent.Future

// Play Framework
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.json.collection._
import play.modules.reactivemongo.ReactiveMongoApi

import reactivemongo.play.json._

/**
  * Model layer to manage quotes
  *
  * @param mongoApi Reference to the Reactive Mongo API
  */
class Quotes(protected val mongoApi: ReactiveMongoApi) {

  /**
    * A reference to the quotes collection of the database.
    *
    * @return
    */
  protected def quotesCollectionJson: JSONCollection = mongoApi.db.collection[JSONCollection]("quotes")


  /**
    * Return the quotes for the given username.
    *
    * @param username The username for which to retrieve quotes
    * @return
    */
  def getQuotes(username: String): Future[Option[JsObject]] = {

    val selector = Json.obj("username" -> username)

    val projector = Json.obj("_id" -> 0)

    quotesCollectionJson.find(selector, projector).one[JsObject]
  }

}
