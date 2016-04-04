package models

import reactivemongo.bson.BSONDocument

object Stats {

  def stats(bookVec: BookVector): BSONDocument = BookStats.stats(bookVec)

}
