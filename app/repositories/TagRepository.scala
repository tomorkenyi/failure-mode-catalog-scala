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
    failureModeCollection.flatMap(_.find(selector).one[FailureMode]).map {
      case Some(fm) =>
        val tagId = BSONObjectID.generate()
        createTag(tag, fm, tagId)
        appendTagToFailureMode(tagId, fm)
        Some(tag)
      case _ => None
    }
  }

  private def createTag(tag: Tag, fm: FailureMode, tagId: BSONObjectID) = {
    val selector = BSONDocument("text" -> tag.text.get)
    tagCollection.flatMap(_.find(selector).one[Tag]).map {
      case None => tagCollection.flatMap(_.insert(tag.copy(_id = Some(tagId), failureModes = Some(Seq(fm._id.get)))))
      case Some(tag) =>
        val appended = tag.failureModes match {
          case None => Some(Seq(fm._id.get))
          case Some(failureModes) =>
            if (failureModes.contains(fm._id.get))
            // TODO this is not called ever...
              throw new IllegalArgumentException("failureMode is already associated with tag")
            else Some(failureModes :+ fm._id.get)
        }
        tagCollection.flatMap(_.update(selector, tag.copy(_id = Some(tagId), failureModes = appended)))
    }
    tag
  }

  private def appendTagToFailureMode(tagId: BSONObjectID, fm: FailureMode): Future[UpdateWriteResult] = {
    val appended = fm.tags match {
      case None => Some(Seq[BSONObjectID](tagId))
      case Some(tags) =>
        if (tags.nonEmpty && tags.contains(tagId))
        // TODO this is not called ever...
          throw new IllegalArgumentException("tag is already associated with failuremode")
        else Some(tags :+ tagId)
    }
    failureModeCollection.flatMap(_.update(BSONDocument("_id" -> fm._id), fm.copy(tags = appended)))
  }

  private def tagCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("tags"))

  private def failureModeCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("failuremodes"))

}
