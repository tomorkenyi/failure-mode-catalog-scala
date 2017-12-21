package controllers

import javax.inject.Inject

import models.FailureMode
import play.api.libs.json.Json
import play.api.mvc._
import reactivemongo.bson.BSONObjectID
import repositories.FailureModeRepository

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future


class FailureModeController @Inject()(cc: ControllerComponents, fmRepo: FailureModeRepository) extends AbstractController(cc) {

  import repositories.JsonFormats._

  def list: Action[AnyContent] = Action.async {
    fmRepo.findAll.map { failureModes =>
      Ok(Json.toJson(failureModes))
    }
  }

  def get(failureModeId: BSONObjectID) = Action.async { req =>
    fmRepo.findById(failureModeId).map { optionalFailureMode =>
      optionalFailureMode.map { failureMode =>
        Ok(Json.toJson(failureMode))
      }.getOrElse(NotFound)
    }
  }

  def create() = Action.async(parse.json) { req =>
    req.body.validate[FailureMode].map { failureMode =>
      fmRepo.create(failureMode).map { _ =>
        Created
      }
    }.getOrElse(Future.successful(BadRequest("Invalid FailureMode is given for create")))
  }

  def update(failureModeId: BSONObjectID) = Action.async(parse.json) { req =>
    req.body.validate[FailureMode].map { failureMode =>
      fmRepo.update(failureModeId, failureMode).map {
        case Some(failureMode) => Ok(Json.toJson(failureMode))
        case None => NotFound
      }
    }.getOrElse(Future.successful(BadRequest("Invalid FailureMode is given for update")))
  }
}
