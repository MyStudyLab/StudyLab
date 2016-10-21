package helpers

import reactivemongo.bson.BSONDocument

/**
  * Created by JordanDodson on 10/21/16.
  */
object Selectors {

  /**
    * Get a selector for the given value of the "username" field
    *
    * @param username
    * @return
    */
  def usernameSelector(username: String) = BSONDocument("username" -> username)

}
