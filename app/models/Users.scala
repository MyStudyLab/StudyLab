package models

// Standard Library
import constructs.{ResultInfo, User}
import play.api.libs.json.Json
import play.modules.reactivemongo.json.collection.JSONCollection

import scala.concurrent.Future

// Play Framework
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// Reactive Mongo
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json._

// Project
import constructs.responses.{ProfilesOnly, AboutMessage, Credentials}
import helpers.Selectors.{emailSelector, usernameSelector}


/**
  * Model layer to manage users
  *
  * @param api Holds the reference to the database.
  */
class Users(protected val api: ReactiveMongoApi) {

  // The users collection
  protected def usersCollection: BSONCollection = api.db.collection[BSONCollection]("users")


  /**
    * Add a new user to the database
    *
    * @param username The user's username
    * @param email    The user's email
    * @param password The user's password
    * @return
    */
  def addNewUser(username: String, email: String, password: String): Future[ResultInfo] = {

    usersCollection.insert(User(username, email, password)).map(result => {
      new ResultInfo(result.ok, result.message)
    })

  }


  /**
    * Get the about message for the user.
    *
    * @param username The username for which to retrieve the data.
    * @return
    */
  def aboutMessage(username: String): Future[Option[AboutMessage]] = {

    usersCollection.find(usernameSelector(username), AboutMessage.projector).one[AboutMessage]
  }


  /**
    * Get the social profiles for the user.
    *
    * @param username The username for which to retrieve data.
    * @return
    */
  def socialProfiles(username: String): Future[Option[ProfilesOnly]] = {

    usersCollection.find(usernameSelector(username), ProfilesOnly.projector).one[ProfilesOnly]
  }


  /**
    * Return true iff the username is already in the database.
    *
    * @param username The username to check for.
    * @return
    */
  def usernameInUse(username: String): Future[Boolean] = {

    usersCollection.count(Some(usernameSelector(username)), limit = 1).map(count => count != 0)
  }


  /**
    * Return true if the email is already in the database.
    *
    * @param email The email address to check for.
    * @return
    */
  def emailInUse(email: String): Future[Boolean] = {

    usersCollection.count(Some(emailSelector(email)), limit = 1).map(count => count != 0)
  }


  /**
    * Check a string against a user's password
    *
    * @param username The username for which to check the password.
    * @param given    The string to check against the user's actual password.
    * @return
    */
  def checkCredentials(username: String, given: String): Future[Boolean] = {

    usersCollection.find(usernameSelector(username), Credentials.projector).one[Credentials].map(optCredentials =>

      optCredentials.fold(false)(credentials => credentials.password == given)
    )
  }


}
