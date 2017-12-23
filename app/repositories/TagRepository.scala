package repositories

import javax.inject.Inject

import models.Tag
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection
import repositories.JsonFormats._

import scala.concurrent.{ExecutionContext, Future}


class TagRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) {

  def create(tag: Tag) = {
    tagCollection.flatMap(_.insert(tag))
  }

  def tagCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("tags"))

}
