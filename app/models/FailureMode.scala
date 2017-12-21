package models

import reactivemongo.bson.BSONObjectID

case class FailureMode(_id: Option[BSONObjectID],
                       functionalState: Option[String],
                       serviceEffect: Option[String],
                       platformEffect: Option[String],
                       potentialCause: Option[String],
                       probability: Option[Int],
                       detectFailures: Option[String],
                       responseAction: Option[String],
                       mitigation: Option[String],
                       detectability: Option[Int],
                       safetyConcern: Option[String],
                       lastUpdated: Option[Long])


