package constructs

import reactivemongo.bson.BSONDocument


/**
  *
  * @param username
  * @param startDay
  * @param startMonth
  * @param startYear
  * @param dailyTotals
  */
case class CumulativeGoal(username: String, startDay: Int, startMonth: Int, startYear: Int, dailyTotals: Vector[Int])


object CumulativeGoal {

  import reactivemongo.bson.Macros

  // Implicitly convert to/from BSON
  implicit val BSONHandler = Macros.handler[CumulativeGoal]

  // A MongoDB projector to get only the fields for this class
  val projector = BSONDocument("_id" -> 0)
}