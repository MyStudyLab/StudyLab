package constructs

import play.api.libs.json.Json
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}


case class ProfilePortfolio(github: String, twitter: String, linkedin: String,
                            stackexchange: String, googleplus: String, ycombinator: String)

object ProfilePortfolio {

  // Implicitly convert from BSON to ProfilePortfolio
  implicit object profilePortfolioReader extends BSONDocumentReader[ProfilePortfolio] {

    def read(bson: BSONDocument): ProfilePortfolio = {
      val opt: Option[ProfilePortfolio] = for {
        profiles <- bson.getAs[BSONDocument]("profiles")
        twitter <- profiles.getAs[String]("twitter")
        github <- profiles.getAs[String]("github")
        linkedin <- profiles.getAs[String]("linkedin")
        stackexchange <- profiles.getAs[String]("stackexchange")
        googleplus <- profiles.getAs[String]("googleplus")
        ycombinator <- profiles.getAs[String]("ycombinator")
      } yield new ProfilePortfolio(github, twitter, linkedin, stackexchange, googleplus, ycombinator)

      // Will throw an error if format is invalid
      opt.get
    }

  }

  // Implicitly convert to JSON
  implicit val ProfilePortfolioWriter = Json.writes[ProfilePortfolio]

  val projector = BSONDocument("profiles" -> 1, "_id" -> 0)

}