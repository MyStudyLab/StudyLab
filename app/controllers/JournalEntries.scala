package controllers

// Standard Library
import javax.inject.Inject

import play.api.libs.json.JsNumber

import scala.concurrent.Future
import scala.concurrent.duration._

// Project
import forms.AddJournalEntryForm
import constructs.{JournalEntry, Point, ResultInfo}

// Play Framework
import play.api.mvc.{Action, Controller}
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits.defaultContext


// Reactive Mongo
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

/**
  * Controller for journal entries
  *
  * @param reactiveMongoApi
  */
class JournalEntries @Inject()(val reactiveMongoApi: ReactiveMongoApi, val ws: WSClient)
  extends Controller with MongoController with ReactiveMongoComponents {

  // Reference to the Journal Entry model
  protected val journalEntries = new models.JournalEntries(reactiveMongoApi)

  /**
    * Invoke the model layer to record a new journal entry.
    *
    * @return
    */
  def addJournalEntry = Action.async { implicit request =>

    withUsername(username => {
      AddJournalEntryForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        goodForm => {

          val cleanedEntry = withoutExcessWhitespace(goodForm.text)

          val position = Point(goodForm.longitude, goodForm.latitude)

          val indicoSentimentURL = "https://apiv2.indico.io/sentiment"

          // TODO: Update the config access
          val indicoRequest = ws.url(indicoSentimentURL)
            .withHeaders("X-ApiKey" -> play.Play.application().configuration().getString("indico_key"))
            .withRequestTimeout(3000.millis)

          val requestPayload = Json.obj(
            "data" -> goodForm.text
          )

          // Send them to indico for sentiment analysis
          indicoRequest.post(requestPayload).flatMap(wsResponse => {

            val score = (wsResponse.json \ "results").getOrElse(JsNumber(0.0)).asOpt[Double].getOrElse(0.0)

            val entry = JournalEntry(username, cleanedEntry, System.currentTimeMillis(), position, score)

            journalEntries.addJournalEntry(entry).map(resultInfo => Ok(resultInfo.toJson))
          })

        }
      )
    })

  }

  /**
    * Get all journal entries for the given username
    *
    * @return
    */
  def getJournalEntries = Action.async { implicit request =>

    withUsername(username =>
      journalEntries.journalEntriesForUsername(username).map(resInfo => Ok(resInfo.toJson))
    )


  }

  /**
    * Get all journal entries in GeoJson format
    *
    * @return
    */
  def getGeoJsonEntries = Action.async { implicit request =>

    val indicoSentimentURL = "https://apiv2.indico.io/sentiment/batch"

    val indicoRequest = ws.url(indicoSentimentURL)
      .withHeaders("X-ApiKey" -> "5c74ed53e9015b5355091e6cac91c303")
      .withRequestTimeout(3000.millis)

    withUsername(username => {

      // First, get journal entries
      journalEntries.journalEntriesForUsername(username).map(resInfo => {

        val geoJson = Json.obj(
          "type" -> "FeatureCollection",
          "features" -> resInfo.payload.map(_.toGeoJson)
        )

        Ok(geoJson)
      })
    })
  }
}
