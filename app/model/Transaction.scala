package model

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.bson.{BSONObjectID, BSONDocument}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._



case class Transaction(id: Option[Long], body: String)

object Transaction {

  import play.api.Play.current
  //db cfg is derived from application.conf
  def collection = ReactiveMongoPlugin.db.collection[JSONCollection]("transactions")

  //convenience converters
  implicit def toOption(s: String): Option[String] = Some(s)
  implicit def toOption(l: Long): Option[Long] = Some(l)

  //formatter to deal with json representations
  implicit val transactionFormat: Format[Transaction] = (
    (__ \ "txId").formatNullable[Long] ~
      (__ \ "body").format[String]
    )(Transaction.apply, unlift(Transaction.unapply))


  def create[T: Format](event: T) = {
    val format = implicitly[Format[T]]
    collection.insert(format.writes(event))
  }

  def list: Future[Seq[Transaction]] = {
    val emptyQuery = BSONDocument()
    val cursor = collection.find(emptyQuery).cursor[Transaction]
    cursor.collect[List]()
  }

  def findById(id: Long): Future[Option[Transaction]] = {
    val cursor = collection.find(Json.obj("id" -> id.toString)).cursor[Transaction]
    cursor.collect[List]().map(list => if (list.isEmpty) None else Some(list(0)))
  }

  val generateId = (__ \ '_id).json.put(Json.parse(BSONObjectID.generate.stringify))

  def create(tx: Transaction): Future[Transaction] = {
    val incomingTx = transactionFormat.writes(tx)
    val addMongoId: Reads[JsObject] = __.json.update(generateId)
    collection.insert(addMongoId.reads(incomingTx)).map(_ => tx)
  }
}

