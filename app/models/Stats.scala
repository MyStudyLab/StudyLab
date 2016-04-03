package models

import reactivemongo.bson.BSONDocument

object Stats {

  def stats(sessionVec: SessionVector): BSONDocument = SessionStats.stats(sessionVec)

  def stats(bookVec: BookVector): BSONDocument = BookStats.stats(bookVec)

}
