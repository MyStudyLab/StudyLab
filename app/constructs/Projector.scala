package constructs

import reactivemongo.bson.BSONDocument


trait Projector[T] {

  def projector: BSONDocument

}