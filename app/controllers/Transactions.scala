package controllers

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{Controller, Action}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

case class Error(code: Int, message: String)

//JsValue is the result of a Json.toJson(_) application, therefore we can put anything in there
//JsValue can be also JsNull, so it absorbs the Option features
// Our response contains a generic result, or an error
case class Response(result: JsValue, error: Option[Error])

//response is a case class with option, so we must write by hand a formatter, but this is easy using functional composition of formatters
object Response {
  implicit val responseFormat: Format[Response] = (
    (__ \ "result").format[JsValue] ~
    (__ \ "error").formatNullable[Error]
    )(Response.apply, unlift(Response.unapply))
}

//for case classes formatters are synthesized automagically
object Error {
  implicit val errorFormat: Format[Error] = Json.format[Error]
}

//we can make a response from any JsValue, or from even any type that provides his Writes type class, so this is totally generic,
object SuccessResponse {
  def apply(sucessResponse: JsValue) = Response(sucessResponse, None)
  //with this we can put in this class any type that can be serialized into JSON, i.e. any type that has an implicit format (or at least a writes)
  def apply[T: Writes](successResponse: T) = {
    val writes = implicitly[Writes[T]]
    Response(writes.writes(successResponse), None)
  }
}

object ErrorResponse {
  def apply(code: Int, message: String) = Response(JsNull, Some(Error(code, message)))
}


class Transactions extends Controller {
  import play.api.Logger
  import model.Transaction
  import model.Transaction._

  val logger = Logger(getClass)

  //return a future of HTTPREsponse, make it async
  def list = Action.async { request =>
    val txFuture: Future[Seq[Transaction]] = Transaction.list
    txFuture.map(txList => Ok(Json.toJson(SuccessResponse(txList))))
  }

  def findById(id: String) = Action.async {
    logger.info(s"findById($id)")

    val txById: Future[Option[Transaction]] = Transaction.findById(id.toLong)

    txById.map( txOption => txOption.fold {
        NotFound(Json.toJson(ErrorResponse(1, "Transaction not found")))
      } {
        tx => Ok(Json.toJson(SuccessResponse(tx)))
      }
    )
  }

  //how to parse request body. this default json parsers works with any body < 512K
  def create = Action.async(parse.json) { request =>

    //JsResult is kind of Either, wrapping the object derived from parsing when successful, or the error
    val incomingBody: JsResult[Transaction] = request.body.validate[Transaction]

    incomingBody.fold (
      invalid = {
        error => {
          val errorMessage = s"Invalid Json, does not represent a transaction. $error"
          Future.successful(BadRequest(Json.toJson(ErrorResponse(0, errorMessage))))
        }
      },
      valid = { tx =>
          logger.info("Creating transaction: " + tx)
          val createdTx: Future[Transaction] = Transaction.create(tx)
          createdTx.map(createdTxValue => Created(Json.toJson(SuccessResponse(createdTxValue))))
    })

  }

}
