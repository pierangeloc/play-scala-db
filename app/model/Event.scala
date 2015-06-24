package model

import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.{Json, Format, Reads}
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.bson.BSONObjectID
import reactivemongo.api._

case class Event(id: Option[String], timestamp: DateTime, eventType: Int, description: String)

object Event {
  implicit val formatter: Format[Event] = (
    (__ \ "id").formatNullable[String] ~
      (__ \ "timestamp").format[DateTime] ~
      (__ \ "eventType").format[Int] ~
      (__ \ "description").format[String]
    )(Event.apply _, unlift(Event.unapply))
}

object MongoDao {
  import play.api.Play.current


  //db cfg is derived from application.conf
  def collection = ReactiveMongoPlugin.db.collection[JSONCollection]("events")

  def create[T: Format](event: T) = {
    val format = implicitly[Format[T]]
    collection.insert(format.writes(event))
  }

  def list[T](from: Int, to: Int) = {
//    collection.find(???).cursor
  }

}
