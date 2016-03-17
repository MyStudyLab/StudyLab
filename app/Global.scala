import play.api._
import play.api.Play.current
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter}
import reactivemongo.api._
import reactivemongo.api.commands.WriteResult

import java.time.{ZonedDateTime, ZoneId}

object Global extends GlobalSettings {

  override def onStart(app: Application): Unit = {

    Logger.info("Application has started")

  }

  // Check if it is a new day, and update stats if so.
  def startTask() = {

    // Connect to database

    val dbURI = sys.env("MONGOLAB_URI")

    val parsedURI = MongoConnection.parseURI(dbURI).get

    val dbName = parsedURI.db.get

    val driver = MongoDriver()

    val conn = driver.connection(parsedURI)

    val db = conn(dbName)

    val collection = db[BSONCollection]("users")

    val selector = BSONDocument()

    Akka.system.scheduler.schedule(0 seconds, 5 minutes) {

      val now = ZonedDateTime.now(ZoneId.of("America/Chicago"))

      if (now.getHour == 12 && now.getMinute > 20) {

      }
    }
  }

}
