package controllers

import model.{MongoDao, Event}
import play.api.Logger
import play.api.mvc.{Action, Controller}


class Events extends Controller {

  val logger = Logger(getClass)

  def create = Action(parse.json) { request =>
    val parsingResult = request.body.validate[Event]

    parsingResult.fold(valid = {
      event => Created("")
    }, invalid = {
      error => BadRequest(s"Body is not an event: $error")
    })
  }

  def list = Action { implicit request =>
    val result = for {
      from  <- request.queryString.get("from")
      to    <- request.queryString.get("to")
    } yield MongoDao.list[Event](from(1).toInt, to(1).toInt)

//    result fold(NotAcceptable("Request invalid"),
//      cursor => Ok())
    result.fold(NotAcceptable("Request invalid")) {
      cursor => Ok("")
    }


  }

  def getById(id: String) = Action {

    Ok("")
  }

}
