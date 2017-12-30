package repositories

import javax.inject.Inject

import models.{FailureMode, Tag}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

object JsonFormats {
  implicit val fmFormat: OFormat[FailureMode] = Json.format[FailureMode]
  implicit val tagFormat: OFormat[Tag] = Json.format[Tag]
}

class FailureModeRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) {
  import JsonFormats._

  def failureModeCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("failuremodes"))

  def findById(id: BSONObjectID): Future[Option[FailureMode]] = {
    val selector = BSONDocument("_id" -> id)
    failureModeCollection.flatMap(_.find(selector).one[FailureMode])
  }

  def findAll: Future[Seq[FailureMode]] = {
    val query = Json.obj()
    failureModeCollection.flatMap(_.find(query)
      .cursor[FailureMode](ReadPreference.primary)
      .collect[Seq]()
    )
  }

  def create(failureMode: FailureMode): Future[WriteResult] = {
    failureModeCollection.flatMap(_.insert(failureMode))
  }

  def update(failureModeId: BSONObjectID, failureMode: FailureMode): Future[Option[FailureMode]] = {
    val selector = BSONDocument("_id" -> failureModeId)
    val updateModifier = BSONDocument(
      "$set" -> BSONDocument(
        "functionalState" -> failureMode.functionalState,
        "serviceEffect" -> failureMode.serviceEffect,
        "platformEffect" -> failureMode.platformEffect,
        "potentialCause" -> failureMode.potentialCause,
        "probability" -> failureMode.probability,
        "detectFailures" -> failureMode.detectFailures,
        "responseAction" -> failureMode.responseAction,
        "mitigation" -> failureMode.mitigation,
        "detectability" -> failureMode.detectability,
        "safetyConcern" -> failureMode.safetyConcern)
    )

    failureModeCollection.flatMap(
      _.findAndUpdate(selector, updateModifier, fetchNewObject = true).map(_.result[FailureMode])
    )
  }

}
