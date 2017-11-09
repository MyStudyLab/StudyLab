package controllers

// Standard Library
import javax.inject.Inject

import constructs.JournalEntryWithID
import forms.{DeleteJournalEntryForm, SetJournalPublicityForm}
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID

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

          // High-Quality Sentiment Analysis
          val indicoMultiURL = "https://apiv2.indico.io/apis/multiapi"

          val indicoRequest = ws.url(indicoMultiURL)
            .withQueryString("apis" -> "sentimenthq,texttags")
            .withRequestTimeout(3000.millis)

          // TODO: Update the config access
          val requestPayload = Json.obj(
            "api_key" -> play.Play.application().configuration().getString("indico_key"),
            "data" -> goodForm.text,
            "top_n" -> 5,
            "threshold" -> 0.1
          )

          // Send them to indico for sentiment analysis
          indicoRequest.post(requestPayload).flatMap(wsResponse => {

            val sentimentScore = (wsResponse.json \ "results" \ "sentimenthq" \ "results").asOpt[Double].getOrElse(0.0)

            val inferredSubjects = (wsResponse.json \ "results" \ "texttags" \ "results").asOpt[Map[String, Double]]
              // Sort by relevance, decreasing
              .map(_.toList.sortBy(-_._2).map(_._1))
              .getOrElse(List())

            val entry = JournalEntryWithID(username, cleanedEntry, System.currentTimeMillis(), position, public = false, sentimentScore, inferredSubjects)

            journalEntries.addJournalEntryWithId(entry).map(resultInfo => Ok(resultInfo.toJson))
          })

        }
      )
    })

  }


  /**
    * Delete a journal entry
    *
    * @return
    */
  def delete = Action.async { implicit request =>

    withUsername(username => {
      DeleteJournalEntryForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        goodForm => {
          BSONObjectID.parse(goodForm.id).toOption.fold(
            Future(Ok(ResultInfo.failWithMessage("Invalid journal entry id").toJson))
          )(oid => journalEntries.delete(username, oid).map(result => Ok(result.toJson)))
        }
      )
    })
  }

  /**
    * Set the publicity of a journal entry
    *
    * @return
    */
  def setPublicity = Action.async { implicit request =>

    withUsername(username => {
      SetJournalPublicityForm.form.bindFromRequest()(request).fold(
        _ => invalidFormResponse,
        goodForm => {
          BSONObjectID.parse(goodForm.id).toOption.fold(
            Future(Ok(ResultInfo.failWithMessage("Invalid journal entry id").toJson))
          )(oid => journalEntries.setPublicity(username, oid, goodForm.public).map(result => Ok(result.toJson)))
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
    * Get all public journal entries for the given username
    *
    * @return
    */
  def getPublicJournalEntries = Action.async { implicit request =>

    request.getQueryString("username").fold(
      Future(ResultInfo.failure("Invalid query string", List[JsObject]()))
    )(username => journalEntries.publicEntries(username)).map(res => Ok(res.toJson))
  }

}
