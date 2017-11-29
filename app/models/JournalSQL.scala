package models

import play.api.db.Database
import anorm._
import anorm.SqlParser._
import constructs.ResultInfo

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class JournalSQL(val db: Database) {


  def add(username: String, text: String, pos: (Double, Double)): Future[ResultInfo[String]] = {

    Future(
      db.withConnection(implicit conn => {

        SQL(
          """
             INSERT INTO journal(user_id, pos, text)
             VALUES ((SELECT id FROM user WHERE username = {username}), point({lat}, {lon}), {text})
          """
        )
          .on("username" -> username, "text" -> text, "lat" -> pos._1, "lon" -> pos._2)
          .execute()
      })
    ).map(success =>
      if (success) ResultInfo.success("Added journal entry", "")
      else ResultInfo.failure("Failed to add journal entry", ""))
  }


  def add(username: String, text: String): Future[Boolean] = {

    Future(
      db.withConnection(implicit conn => {

        SQL(
          """
             SET @user_id = (SELECT id FROM user WHERE username = {username});

             INSERT INTO journal(user_id, pos, text)
             VALUES (@user_id, {text})
        """
        ).on("username" -> username, "text" -> text)

        ???
      })
    )
  }


  def delete(journalID: Int, username: String): Future[Boolean] = {

    Future(
      db.withConnection(implicit conn => {

        SQL(
          """
             SET @user_id = (SELECT id FROM user WHERE username = {username});

             DELETE FROM journal
             WHERE id = {id} AND user_id = @user_id
        """
        ).on("username" -> username, "id" -> journalID)


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
