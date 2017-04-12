package constructs

// Play Framework
import play.api.libs.json.Json

/**
  *
  * Encoder/Decoder case class for user profile data
  *
  * @param github        Url of github profile
  * @param twitter       Url of twitter profile
  * @param linkedin      Url of linkedin profile
  * @param stackexchange Url of Stack Exchange profile
  * @param googleplus    Url of google+ profile
  * @param ycombinator   Url of Hacker News profile
  * @param goodreads     Url of goodreads profile
  * @param codewars      Url of codewars profile
  * @param freecodecamp  Url of Free Code Camp profile
  * @param quora         Url of quora profile
  * @param pinterest     Url of pinterest profile
  * @param facebook      Url of facebook profile
  * @param codecademy    Url of Codecademy profile
  * @param coursera      Url of Coursera profile
  * @param imdb          Url of IMDb profile
  * @param gyroscope     Url of Gyroscope profile
  * @param duolingo      Url of Duolingo profile
  * @param wordpress     Url of Wordpress profile
  */
case class Profiles(github: String, twitter: String, linkedin: String,
                    stackexchange: String, googleplus: String, ycombinator: String,
                    goodreads: String, codewars: String, freecodecamp: String,
                    quora: String, pinterest: String, facebook: String,
                    codecademy: String, coursera: String, imdb: String,
                    gyroscope: String, duolingo: String, wordpress: String)

/**
  * Companion object where handlers and writers are defined
  */
object Profiles {

  import reactivemongo.bson.Macros

  val emptyProfiles = Profiles("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")

  // Implicitly convert to/from BSON
  implicit val ProfilesHandler = Macros.handler[Profiles]


  // Implicitly convert to JSON
  implicit val ProfilePortfolioWriter = Json.writes[Profiles]

}