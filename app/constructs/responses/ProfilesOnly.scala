package constructs.responses

import constructs.Profiles
import play.api.libs.json.Json
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}

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
  implicit val ProfilesOnlyWriter = Json.writes[ProfilesOnly]

  val projector = BSONDocument("contactInfo.profiles" -> 1, "_id" -> 0)
}