package constructs


case class ContactInfo(firstName: String, lastName: String, middleName: String, email: String,
                       phone: String, profiles: Profiles)

object ContactInfo {

  import reactivemongo.bson.Macros

  // Implicitly convert to/from BSON
  implicit val ContactInfoHandler = Macros.handler[ContactInfo]
}
