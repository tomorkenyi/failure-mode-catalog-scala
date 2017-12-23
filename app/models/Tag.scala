package models

import reactivemongo.bson.BSONObjectID

case class Tag(_id: Option[BSONObjectID], text: Option[String], colorCode: Option[String])
