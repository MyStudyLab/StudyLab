package models

import play.api.db.Database
import anorm._
import anorm.SqlParser._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class JournalSQL(val db: Database) {


  def add(userID: Int, text: String): Future[Boolean] = {

    Future(
      db.withConnection(implicit conn => {

        SQL(
          """
           INSERT INTO journal(user_id, text)
           VALUES ({user_id}, {text})
        """
        ).on("user_id" -> userID, "text" -> text)

        ???
      })
    )
  }


  
  def delete(username: String): Future[Boolean] = {

    Future(
      db.withConnection(implicit conn => {

        SQL(
          """
           DELETE FROM journal
           WHERE id = (SELECT id FROM user WHERE username = {username})
        """
        ).on("username" -> username)


        ???
      })
    )
  }


  def togglePublicity(id: Int): Future[Boolean] = {

    Future(
      db.withConnection(implicit conn => {

        SQL(
          """
             UPDATE journal
             SET publicity = NOT publicity
             WHERE id = {id}
          """
        ).on("id" -> id)

        ???
      })
    )

  }


  def getAll(username: String): Future[List[String]] = {

    Future(
      db.withConnection(implicit conn => {

        SQL(
          """
           SELECT (j.text, j.time)
           FROM journal j JOIN user u ON j.user_id = u.id
           WHERE u.username = {username}
        """
        ).on("username" -> username)

        ???
      })
    )
  }


  def getPublic(username: String): Future[List[String]] = {

    Future(

      db.withConnection(implicit conn => {

        SQL(
          """
           SELECT (j.text, j.time)
           FROM journal j JOIN user u ON j.user_id = u.id
           WHERE u.username = {username} AND j.public = TRUE
        """
        ).on("username" -> username)

        ???
      })

    )

  }

}
