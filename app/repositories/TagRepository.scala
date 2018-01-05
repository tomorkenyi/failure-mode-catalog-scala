package repositories

import javax.inject.Inject

import models.{FailureMode, Tag}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}


class TagRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) {

  import JsonFormats._

  def create(tag: Tag, failureModeId: BSONObjectID): Future[Option[Tag]] = {
    failureModeCollection.flatMap(_.find(BSONDocument("_id" -> failureModeId)).one[FailureMode]).flatMap {
      case Some(failureMode) => createTagIfFailureModeExists(tag, failureMode)
      case _ => Future(None)
    }
  }

  def search(text: String): Future[Option[Tag]] = {
    tagCollection.flatMap(_.find(BSONDocument("text" -> text)).one[Tag])
  }

  private def createTagIfFailureModeExists(tag: Tag, failureMode: FailureMode): Future[Option[Tag]] = {
    val tagId = BSONObjectID.generate()
    createTag(tag.copy(_id = Some(tagId)), failureMode._id.get).map {
      case Some(resultTag) => appendTagToFailureMode(resultTag, failureMode)
      case None => None
    }
  }

  private def createTag(tag: Tag, failureModeId: BSONObjectID): Future[Option[Tag]] = {
    tagCollection.flatMap(_.find(BSONDocument("text" -> tag.text.get)).one[Tag]).flatMap {
      case Some(dbTag) => appendFailureModeToTag(dbTag, failureModeId)
      case None => insertNewTagWithFirstFailureMode(tag, failureModeId)
    }
  }

  private def appendFailureModeToTag(tag: Tag, fmId: BSONObjectID): Future[Option[Tag]] = {
    val appendedFailureModes = tag.failureModes match {
      case Some(failureModes) => if (failureModes.contains(fmId)) Some(failureModes) else Some(failureModes :+ fmId)
      case None => Some(Seq(fmId))
    }
    updateExistingTagWithFailureModes(tag, appendedFailureModes)
  }

  private def updateExistingTagWithFailureModes(tag: Tag, appendedFailureModes: Option[Seq[BSONObjectID]]) = {
    val updatedTag = tag.copy(failureModes = appendedFailureModes)
    tagCollection.flatMap(_.update(BSONDocument("text" -> tag.text.get), updatedTag))
    Future.successful(Some(updatedTag))
  }

  private def insertNewTagWithFirstFailureMode(tag: Tag, failureModeId: BSONObjectID): Future[Option[Tag]] = {
    val createdTag = tag.copy(failureModes = Some(Seq(failureModeId)))
    tagCollection.flatMap(_.insert(createdTag))
    Future.successful(Some(createdTag))
  }

  private def appendTagToFailureMode(tag: Tag, failureMode: FailureMode): Option[Tag] = {
    val appendedTags = failureMode.tags match {
      case Some(tags) if !tags.contains(tag._id.get) => Some(tags :+ tag._id.get)
      case _ => Some(Seq[BSONObjectID](tag._id.get))
    }
    failureModeCollection.flatMap(_.update(BSONDocument("_id" -> failureMode._id), failureMode.copy(tags = appendedTags)))
    Some(tag)
  }

  private def tagCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("tags"))

  private def failureModeCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("failuremodes"))

}
