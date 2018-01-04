package repositories

import javax.inject.Inject

import models.{FailureMode, Tag}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}


class TagRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) {

  import JsonFormats._

  def create(tag: Tag, failureModeId: BSONObjectID): Future[Option[Tag]] = {
    val selector = BSONDocument("_id" -> failureModeId)
    failureModeCollection.flatMap(_.find(selector).one[FailureMode]).flatMap {
      case Some(fm) =>
        val tagId = BSONObjectID.generate()
        createTag(tag, fm, tagId).map {
          case Some(result) =>
            appendTagToFailureMode(tagId, fm)
            Some(result)
          case None => None
        }
      case _ => Future(None)
    }
  }

  private def createTag(tag: Tag, fm: FailureMode, tagId: BSONObjectID): Future[Option[Tag]] = {
    val selector = BSONDocument("text" -> tag.text.get)
    tagCollection.flatMap(_.find(selector).one[Tag]).flatMap {
      case Some(tag) =>
        val appended = tag.failureModes match {
          case None => Some(Seq(fm._id.get))
          case Some(failureModes) => if (failureModes.contains(fm._id.get)) Some(failureModes) else Some(failureModes :+ fm._id.get)
        }
        val updatedTag = tag.copy(_id = Some(tagId), failureModes = appended)
        tagCollection.flatMap(_.update(selector, updatedTag))
        Future.successful(Some(updatedTag))
      case None =>
        val createdTag: Tag = tag.copy(_id = Some(tagId), failureModes = Some(Seq(fm._id.get)))
        tagCollection.flatMap(_.insert(createdTag))
        Future.successful(Some(createdTag))
    }
  }

  private def appendTagToFailureMode(tagId: BSONObjectID, fm: FailureMode): Future[UpdateWriteResult] = {
    val appended = fm.tags match {
      case Some(tags) if !tags.contains(tagId) => Some(tags :+ tagId)
      case _ => Some(Seq[BSONObjectID](tagId))
    }
    failureModeCollection.flatMap(_.update(BSONDocument("_id" -> fm._id), fm.copy(tags = appended)))
  }

  private def tagCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("tags"))

  private def failureModeCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("failuremodes"))

}
