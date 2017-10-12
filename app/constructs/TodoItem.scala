package constructs

import play.api.libs.json.Json
import reactivemongo.bson.BSONDocument

case class TodoItem(username: String, text: String, startTime: Long, endTime: Long, startPos: Point, endPos: Point)

object TodoItem {

  def apply(username: String, text: String, startTime: Long, startPos: Point): TodoItem = new TodoItem(username, text, startTime, 0, startPos, Point(0, 0))

  // Implicitly convert to JSON
  implicit val todoItemWrites = Json.writes[TodoItem]

  import reactivemongo.bson.Macros

  // Implicitly convert to/from BSON
  implicit val BSONHandler = Macros.handler[TodoItem]

  val projector = BSONDocument("_id" -> 1, "username" -> 1, "text" -> 1, "startTime" -> 1, "endTime" -> 1,
    "startPos" -> 1, "endPos" -> 1)

}
