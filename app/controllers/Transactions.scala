package controllers

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{Controller, Action}


case class Transaction(id: Option[Long], body: String)

case class Error(code: Int, message: String)
//JsValue is the result of a Json.toJson(_) application, therefore we can put anything in there
//JsValue can be also JsNull, so it absorbs the Option features
case class Response(result: JsValue, error: Option[Error])

object Transaction {
  //convenience converters
  implicit def toOption(s: String): Option[String] = Some(s)
  implicit def toOption(l: Long): Option[Long] = Some(l)

  implicit val transactionFormat: Format[Transaction] = (
    (__ \ "id").formatNullable[Long] ~
    (__ \ "body").format[String]
    )(Transaction.apply, unlift(Transaction.unapply))
}

//TODO: use only writers, formats are overkill for Response
object Response {
  implicit val responseFormat: Format[Response] = (
    (__ \ "result").format[JsValue] ~
    (__ \ "error").formatNullable[Error]
    )(Response.apply, unlift(Response.unapply))
}

object Error {
  implicit val errorFormat: Format[Error] = Json.format[Error]
}

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
  import Transaction._

  val logger = Logger(getClass)
  var transactions: Seq[Transaction] = List(Transaction(1L, "you must pay"))

  def list = Action { request =>
    Ok(Json.toJson(SuccessResponse(transactions)))
  }

  def findById(id: String) = Action {
    logger.info(s"findById($id)")
    logger.info(transactions.find(_.id == Some(id.toLong)).toString)

    transactions.find(_.id == Some(id.toLong)).fold {
      NotFound(Json.toJson(ErrorResponse(1, "Transaction not found")))
    } {
      tx => Ok(Json.toJson(SuccessResponse(tx)))
    }
  }

  //how to parse request body. this default json parsers works with any body < 512K
  def create = Action(parse.json) { request =>

    //JsResult is kind of Option, wrapping the object derived from parsing
    val incomingBody: JsResult[Transaction] = request.body.validate[Transaction]

    incomingBody.fold (
      invalid = {
        error => {
          val errorMessage = s"Invalid Json, does not represent a transaction. $error"
          BadRequest(Json.toJson(ErrorResponse(0, errorMessage)))
        }
      },
      valid = { tx =>
          logger.info("Creating transaction: " + tx)

          val createdTx: Transaction = tx
        Created(Json.toJson(SuccessResponse(createdTx)))
    })

  }

}
