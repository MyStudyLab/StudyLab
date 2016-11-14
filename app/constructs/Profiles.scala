package constructs

import play.api.libs.json.Json

/**
  *
  * @param github
  * @param twitter
  * @param linkedin
  * @param stackexchange
  * @param googleplus
  * @param ycombinator
  * @param goodreads
  * @param codewars
  * @param freecodecamp
  * @param quora
  * @param pinterest
  * @param facebook
  * @param codecademy
  * @param coursera
  */
case class Profiles(github: String, twitter: String, linkedin: String,
                    stackexchange: String, googleplus: String, ycombinator: String,
                    goodreads: String, codewars: String, freecodecamp: String,
                    quora: String, pinterest: String, facebook: String,
                    codecademy: String, coursera: String)

/**
  *
  */
object Profiles {

  import reactivemongo.bson.Macros

  // Implicitly convert to/from BSON
  implicit val ProfilesHandler = Macros.handler[Profiles]


  // Implicitly convert to JSON
  implicit val ProfilePortfolioWriter = Json.writes[Profiles]

}