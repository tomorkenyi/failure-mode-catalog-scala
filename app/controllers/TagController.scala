package controllers

import javax.inject.Inject

import models.Tag
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import reactivemongo.bson.BSONObjectID
import repositories.TagRepository

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class TagController @Inject()(cc: ControllerComponents, tagRepo: TagRepository) extends AbstractController(cc) {

  import repositories.JsonFormats._

  def create(failureModeId: BSONObjectID): Action[JsValue] = Action.async(parse.json) { req =>
    req.body.validate[Tag].map { tag =>
      tagRepo.create(tag, failureModeId).map {
        case Some(resultTag) => Created(Json.toJson(resultTag))
        case None => InternalServerError("Tag cannot be created")
      }
    }.getOrElse(Future.successful(BadRequest("Invalid FailureMode is given for create")))
  }
}
