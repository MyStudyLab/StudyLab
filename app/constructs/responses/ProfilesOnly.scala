package constructs.responses

// Project
import constructs.Profiles

// Play Framework
import play.api.libs.json.{Json, Writes}

// Reactive Mongo
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}

/**
  *
  * @param profiles The profiles for a user
  */
case class ProfilesOnly(profiles: Profiles)

object ProfilesOnly {

  // Implicitly convert from BSON to ProfilePortfolio
  implicit object profilePortfolioReader extends BSONDocumentReader[ProfilesOnly] {

    def read(bson: BSONDocument): ProfilesOnly = {
      val opt: Option[ProfilesOnly] = for {
        contactInfo <- bson.getAs[BSONDocument]("contactInfo")
        profiles <- contactInfo.getAs[Profiles]("profiles")
      } yield new ProfilesOnly(profiles)

      // Will throw an error if format is invalid
      opt.get
    }

  }

  // Implicitly convert to JSON
  implicit val ProfilesOnlyWriter: Writes[ProfilesOnly] = Json.writes[ProfilesOnly]

  val projector = BSONDocument("username" -> 1, "contactInfo.profiles" -> 1, "_id" -> 0)
}