package constructs


case class ContactInfo(firstName: String, lastName: String, middleName: String, email: String,
                       phone: String, profiles: Profiles)

object ContactInfo {

  import reactivemongo.bson.Macros

  /**
    * Create a ContactInfo object with only an email
    *
    * @param email
    * @return
    */
  def onlyEmail(email: String) = ContactInfo("", "", "", email, "", Profiles.emptyProfiles)

  // Implicitly convert to/from BSON
  implicit val ContactInfoHandler = Macros.handler[ContactInfo]
}
