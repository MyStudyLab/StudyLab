package constructs

import play.api.libs.json.{JsValue, Json, Writes}
import reactivemongo.bson.{BSONDocument, BSONHandler}

/**
  *
  * @param lon
  * @param lat
  */
case class Point(lon: Double, lat: Double)

object Point {

  // Implicitly convert to JSON
  implicit val pointWrites = new Writes[Point] {

    override def writes(o: Point): JsValue = {
      Json.obj(
        "type" -> "Point",
        "coordinates" -> List(o.lon, o.lat)
      )
    }

  }


  // Implicitly convert to/from BSON, using the GeoJson format
  implicit object PointWriter extends BSONHandler[BSONDocument, Point] {

    def write(p: Point): BSONDocument = {
      BSONDocument(
        "type" -> "Point",
        "coordinates" -> List(p.lon, p.lat)
      )
    }

    def read(bson: BSONDocument): Point = {

      try {
        val coords = bson.getAs[Vector[Double]]("coordinates").get
        Point(coords(0), coords(1))
      } catch {
        case _: Throwable => throw new Exception("Error while parsing Point")
      }
    }

  }

}
