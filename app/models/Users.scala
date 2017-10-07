package models

// Standard Library
import reactivemongo.bson.BSONDocument

import scala.concurrent.Future

// Play Framework
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// Reactive Mongo
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json._

// Project
import constructs.{ResultInfo, User}
import constructs.responses.{ProfilesOnly, AboutMessage, Credentials}
import helpers.Selectors.{emailSelector, usernameSelector}


/**
  * Model layer to manage users
  *
  * @param api Holds the reference to the database.
  */
class Users(protected val api: ReactiveMongoApi) {

  // The users collection
  protected def usersCollection: Future[BSONCollection] = api.database.map(_.collection[BSONCollection]("users"))


  /**
    * Add a new user to the database
    *
    * @param user The user to add
    * @return
    */
  def addNewUser(user: User): Future[ResultInfo[String]] = {

    usersCollection.flatMap(_.insert(user)).map(result =>
      ResultInfo(result.ok, "used to be result.message")
    ).recover {
      case e: DatabaseException => e.code.fold(ResultInfo.failWithMessage(e.message)) {

        // A unique index was violated
        case 11000 =>
          if (e.message.contains("username_1")) ResultInfo.failWithMessage("username already in use")
          else ResultInfo.failWithMessage("email already in use")

        // TODO: add cases for other error codes

        // Catch all the other error codes
        case _ => ResultInfo.failWithMessage(e.message)
      }
    }
  }


  /**
    * Remove a user from the database
    *
    * @param username The username of the user to be removed
    * @return
    */
  def deleteUser(username: String, password: String): Future[ResultInfo[String]] = {

    checkCredentials(username, password).flatMap(validated => {

      if (validated) {
        usersCollection.flatMap(_.remove(usernameSelector(username), firstMatchOnly = true)).map(
          result =>
            if (result.ok) ResultInfo.succeedWithMessage("Successfully deleted user account")
            else ResultInfo.failWithMessage("Failed to delete user account")
        )
      } else {
        Future(ResultInfo.badUsernameOrPass)
      }
    })
  }


  /**
    * Get the about message for the user.
    *
    * @param username The username for which to retrieve the data.
    * @return
    */
  def aboutMessage(username: String): Future[Option[AboutMessage]] = {

    usersCollection.flatMap(_.find(usernameSelector(username), AboutMessage.projector).one[AboutMessage])
  }


  /**
    * Get the social profiles for the user.
    *
    * @param username The username for which to retrieve data.
    * @return
    */
  def socialProfiles(username: String): Future[Option[ProfilesOnly]] = {

    usersCollection.flatMap(_.find(usernameSelector(username), ProfilesOnly.projector).one[ProfilesOnly])
  }


  /**
    * Return true iff the username is already in the database.
    *
    * @param username The username to check for.
    * @return
    */
  def usernameInUse(username: String): Future[Boolean] = {

    usersCollection.flatMap(_.count(Some(usernameSelector(username)), limit = 1)).map(count => count != 0)
  }


  /**
    * Return true if the email is already in the database.
    *
    * @param email The email address to check for.
    * @return
    */
  def emailInUse(email: String): Future[Boolean] = {

    usersCollection.flatMap(_.count(Some(emailSelector(email)), limit = 1)).map(count => count != 0)
  }


  /**
    * Check a string against a user's password
    *
    * @param username The username for which to check the password.
    * @param given    The string to check against the user's actual password.
    * @return
    */
  def checkCredentials(username: String, given: String): Future[Boolean] = {

    usersCollection.flatMap(_.find(usernameSelector(username), Credentials.projector).one[Credentials]).map(optCredentials =>

      optCredentials.fold(false)(credentials => credentials.password == given)
    )
  }

  /**
    * Change a user's password
    *
    * @param username    The username for which to change the password
    * @param newPassword The user's new password
    * @return
    */
  def changePassword(username: String, newPassword: String): Future[Boolean] = {

    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "password" -> newPassword
      )
    )

    usersCollection.flatMap(_.update(usernameSelector(username), modifier)).map(_.ok)
  }


  /**
    * Change a user's email address
    *
    * @param username The username for which to change the email
    * @param newEmail The user's new email
    * @return
    */
  def changeEmail(username: String, newEmail: String): Future[Boolean] = {

    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "contactInfo.email" -> newEmail
      )
    )

    usersCollection.flatMap(_.update(usernameSelector(username), modifier)).map(_.ok)
  }

}
