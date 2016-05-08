package models

import helpers.ResultInfo
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext


/**
  * Model layer to manage study sessions and subjects.
  *
  * @param mongoApi Holds the reference to the database.
  */
class Sessions(val mongoApi: ReactiveMongoApi) {

  // An interface to the sessions collection as BSON
  def bsonSessionsCollection: BSONCollection = mongoApi.db.collection[BSONCollection]("sessions")

  // A result indicating that the user was already studying.
  val alreadyStudying = ResultInfo(success = false, "Already studying")

  // A result indicating that the user was not studying.
  val notStudying = ResultInfo(success = false, "Not studying")

  // A result indicating that the given subject was invalid.
  val invalidSubject = ResultInfo(success = false, "Invalid subject")


  /**
    * Get study stats as JSON.
    *
    * @param user_id The user ID for which to get study stats.
    * @return
    */
  def getStats(user_id: Int): Future[Option[Stats]] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("user_id" -> 1, "subjects" -> 1, "status" -> 1, "sessions" -> 1, "_id" -> 0)

    bsonSessionsCollection.find(selector, projector).one[SessionData].map(optData =>
      optData.map(sessionData => Stats.compute(sessionData.sessions, sessionData.status))
    )
  }


  /**
    * Starts a study session for the given user and subject.
    *
    * @param user_id The user ID for which to start a new session.
    * @param subject The subject of the new study session.
    * @return
    */
  def startSession(user_id: Int, subject: String): Future[ResultInfo] = {

    // Find the document with the given user id
    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("user_id" -> 1, "status" -> 1, "subjects" -> 1, "_id" -> 0)

    bsonSessionsCollection.find(selector, projector).one[StatusAndSubjects].flatMap(opt =>

      opt.fold(Future(ResultInfo.badUsernameOrPass))(statsAndSubs => {

        if (statsAndSubs.status.isStudying) Future(alreadyStudying)
        else if (!statsAndSubs.subjects.map(_.name).contains(subject)) Future(invalidSubject)
        else {

          // The modifier needed to start a session
          val modifier = BSONDocument(
            "$set" -> BSONDocument(
              "status.isStudying" -> true,
              "status.subject" -> subject,
              "status.start" -> System.currentTimeMillis()
            )
          )

          // Update the status
          bsonSessionsCollection.update(selector, modifier, multi = false).map(result =>
            if (result.ok) ResultInfo(success = true, s"Now studying $subject")
            else ResultInfo.databaseError
          )
        }
      })
    )
  }


  /**
    * Stops the current study session.
    *
    * @param user_id The user ID for which to stop the current session.
    * @param message The commit message for the study session.
    * @return
    */
  def stopSession(user_id: Int, message: String): Future[ResultInfo] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("user_id" -> 1, "subjects" -> 1, "status" -> 1, "sessions" -> 1, "_id" -> 0)

    bsonSessionsCollection.find(selector, projector).one[SessionData].flatMap { opt =>

      opt.fold(Future(ResultInfo.badUsernameOrPass))(sessionData => {

        if (!sessionData.status.isStudying) Future(notStudying)
        else {

          val newSession = Session(sessionData.status.subject, sessionData.status.start, System.currentTimeMillis(), message)

          // The modifier needed to stop a session
          val modifier = BSONDocument(
            "$set" -> BSONDocument(
              "status.isStudying" -> false
            ),
            "$push" -> BSONDocument(
              "sessions" -> newSession
            )
          )

          // Add the new session and updated stats
          bsonSessionsCollection.update(selector, modifier, multi = false).map(result =>
            if (result.ok) ResultInfo(success = true, "Finished studying")
            else ResultInfo.databaseError)
        }
      })
    }
  }


  /**
    * Aborts the current study session.
    *
    * @param user_id The user ID for which to abort the current session.
    * @return
    */
  def abortSession(user_id: Int): Future[ResultInfo] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("user_id" -> 1, "status" -> 1, "subjects" -> 1, "_id" -> 0)

    bsonSessionsCollection.find(selector, projector).one[StatusAndSubjects].flatMap { opt =>

      opt.fold(Future(ResultInfo.badUsernameOrPass))(statsAndSubs => {

        if (!statsAndSubs.status.isStudying) Future(notStudying)
        else {

          // The modifier needed to abort a session
          val modifier = BSONDocument(
            "$set" -> BSONDocument(
              "status.isStudying" -> false
            )
          )

          // Update the status
          bsonSessionsCollection.update(selector, modifier, multi = false).map(result =>
            if (result.ok) ResultInfo(success = true, "Study session aborted")
            else ResultInfo.databaseError)
        }
      })
    }
  }


  /**
    * Adds the given subject to the user's subject list.
    *
    * @param user_id The user ID for which to add the given subject.
    * @param subject The new subject.
    * @return
    */
  def addSubject(user_id: Int, subject: String): Future[ResultInfo] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("user_id" -> 1, "status" -> 1, "subjects" -> 1, "_id" -> 0)

    // Get the user's session data
    bsonSessionsCollection.find(selector, projector).one[StatusAndSubjects].flatMap { opt =>

      // Check the success of the query
      opt.fold(Future(ResultInfo.badUsernameOrPass))(statsAndSubs => {

        if (statsAndSubs.subjects.map(_.name).contains(subject)) Future(ResultInfo(success = true, "Subject already added."))
        else {


          // The modifier needed to add a subject
          val modifier = BSONDocument(
            "$push" -> BSONDocument(
              "subjects" -> Subject(subject, System.currentTimeMillis(), isLanguage = false, "")
            )
          )

          // Add the new subject
          bsonSessionsCollection.update(selector, modifier, multi = false).map(result =>
            if (result.ok) ResultInfo(success = true, s"Successfully added $subject.")
            else ResultInfo.databaseError)
        }
      })
    }
  }


  /**
    * Removes the given subject from the user's subject list.
    *
    * @param user_id The user ID for which to remove the subject.
    * @param subject The subject to remove.
    * @return
    */
  def removeSubject(user_id: Int, subject: String): Future[ResultInfo] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("user_id" -> 1, "subjects" -> 1, "status" -> 1, "sessions" -> 1, "_id" -> 0)

    // Get the user's session data
    bsonSessionsCollection.find(selector, projector).one[SessionData].flatMap { opt =>

      // Check the success of the query
      opt.fold(Future(ResultInfo.badUsernameOrPass))(sessionData => {

        if (!sessionData.subjects.map(_.name).contains(subject)) {
          Future(ResultInfo(success = false, "Invalid subject."))
        } else if (sessionData.sessions.map(_.subject).toSet.contains(subject)) {
          Future(ResultInfo(success = false, s"Can't remove $subject. It has been studied."))
        } else {

          // New subject vector without the subject.
          val newSubjects = sessionData.subjects.filterNot(_.name == subject)

          // The modifier needed to add a subject
          val modifier = BSONDocument(
            "$set" -> BSONDocument(
              "subjects" -> newSubjects
            )
          )

          // Remove the subject
          bsonSessionsCollection.update(selector, modifier, multi = false).map(result =>
            if (result.ok) ResultInfo(success = true, s"Successfully removed $subject.")
            else ResultInfo.databaseError)
        }
      })
    }
  }


  /**
    * Rename an existing subject.
    *
    * @param user_id The user ID for which to rename the given subject.
    * @param oldName The subject to rename.
    * @param newName The new name.
    * @return
    */
  def renameSubject(user_id: Int, oldName: String, newName: String): Future[ResultInfo] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("user_id" -> 1, "subjects" -> 1, "status" -> 1, "sessions" -> 1, "_id" -> 0)

    // Get the user's session data
    bsonSessionsCollection.find(selector, projector).one[SessionData].flatMap { opt =>

      // Check the success of the query
      opt.fold(Future(ResultInfo.badUsernameOrPass))(sessionData => {

        sessionData.subjects.find(_.name == oldName).fold(
          Future(ResultInfo(success = false, s"$oldName is an invalid subject."))
        )(oldSub => {

          // Check if the new subject name is already in use
          if (sessionData.subjects.map(_.name).contains(newName)) {
            Future(ResultInfo(success = false, s"Can't rename to $newName. It is an existing subject."))
          } else {

            val newSub = Subject(newName, oldSub.added, oldSub.isLanguage, oldSub.description)

            // Updated subject list using the new subject name
            val newSubjects = sessionData.subjects.filterNot(_.name == oldName) :+ newSub

            // Updated session data using the new subject name
            val newSessions = sessionData.sessions.map(session => {
              if (session.subject == oldName) Session(newName, session.startTime, session.endTime, session.message)
              else session
            })

            // The modifier needed to rename a subject
            val modifier = BSONDocument(
              "$set" -> BSONDocument(
                "subjects" -> newSubjects,
                "sessions" -> newSessions
              )
            )

            // Add the new subject
            bsonSessionsCollection.update(selector, modifier, multi = false).map(result =>
              if (result.ok) ResultInfo(success = true, s"Successfully renamed $oldName to $newName.")
              else ResultInfo.databaseError)
          }
        })
      })
    }
  }


  /**
    * Merge two subjects, combining their sessions.
    *
    * TODO: Needs testing
    *
    * @param user_id   The user ID for which to merge the subjects.
    * @param absorbed  The subject that will be absorbed.
    * @param absorbing The subject that will absorb the other.
    * @return
    */
  def mergeSubjects(user_id: Int, absorbed: String, absorbing: String): Future[ResultInfo] = {

    val selector = BSONDocument("user_id" -> user_id)

    val projector = BSONDocument("user_id" -> 1, "subjects" -> 1, "status" -> 1, "sessions" -> 1, "_id" -> 0)

    // Get the user's session data
    bsonSessionsCollection.find(selector, projector).one[SessionData].flatMap { opt =>

      // Check the success of the query
      opt.fold(Future(ResultInfo.badUsernameOrPass))(data => {

        val subjectNames = data.subjects.map(_.name)

        if (!subjectNames.contains(absorbed)) {
          Future(ResultInfo(success = true, s"Can't merge. $absorbed is an invalid subject."))
        } else if (!subjectNames.contains(absorbing)) {
          Future(ResultInfo(success = true, s"Can't merge. $absorbing is an invalid subject."))
        } else {

          // Updated subject vector without the absorbed subject name
          val newSubjects = data.subjects.filterNot(_.name == absorbed)

          // Updated session vector without the absorbed subject name
          val newSessions = data.sessions.map(session => {
            if (session.subject == absorbed) Session(absorbing, session.startTime, session.endTime, session.message)
            else session
          })

          // The modifier needed to merge the subjects
          val modifier = BSONDocument(
            "$set" -> BSONDocument(
              "subjects" -> newSubjects,
              "sessions" -> newSessions
            )
          )

          // Merge the subjects
          bsonSessionsCollection.update(selector, modifier, multi = false).map(result =>
            if (result.ok) ResultInfo(success = true, s"Successfully merged $absorbed into $absorbing.")
            else ResultInfo.databaseError)
        }
      })
    }
  }


}

