package constructs


case class Domain(name: String, subjects: Vector[Subject], added: Long, description: String)

object Domain {

  import reactivemongo.bson.Macros

  // Implicitly converts to/from BSON
  implicit val DomainHandler = Macros.handler[Domain]

}