package controllers

import javax.inject.Inject

import models.FailureMode
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import reactivemongo.bson.BSONObjectID
import repositories.FailureModeRepository

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future


class FailureModeController @Inject()(cc: ControllerComponents, failureModeRepo: FailureModeRepository) extends AbstractController(cc) {

  import repositories.JsonFormats._

  def list: Action[AnyContent] = Action.async {
    failureModeRepo.findAll.map { failureModes =>
      Ok(Json.toJson(failureModes))
    }
  }

  def get(failureModeId: BSONObjectID): Action[AnyContent] = Action.async { req =>
    failureModeRepo.findById(failureModeId).map { optionalFailureMode =>
      optionalFailureMode.map { failureMode =>
        Ok(Json.toJson(failureMode))
      }.getOrElse(NotFound)
    }
  }

  def create(): Action[JsValue] = Action.async(parse.json) { req =>
    req.body.validate[FailureMode].map { failureMode =>
      failureModeRepo.create(failureMode).map { _ =>
        Created
      }
    }.getOrElse(Future.successful(BadRequest("Invalid FailureMode JSON object is given for creating a failure mode!")))
  }

  def update(failureModeId: BSONObjectID): Action[JsValue] = Action.async(parse.json) { req =>
    req.body.validate[FailureMode].map { failureMode =>
      failureModeRepo.update(failureModeId, failureMode).map {
        case Some(failureModeResult) => Ok(Json.toJson(failureModeResult))
        case None => NotFound
      }
    }.getOrElse(Future.successful(BadRequest("Invalid FailureMode JSON object is given for updating a failure mode!")))
  }
}
