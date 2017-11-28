package models

import play.api.db.Database

class JournalSQL(val db: Database) {


  def getAll(username: String): List[String] = {

    db.withConnection(conn => {

      ???
    })
  }

}
