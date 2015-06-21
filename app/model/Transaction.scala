package model

import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.functional.syntax._
import play.api.libs.json._
import slick.driver.JdbcProfile
import play.api.Play.current

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._


case class Transaction(id: Option[Long], body: String)

object Transaction {

  //convenience converters
  implicit def toOption(s: String): Option[String] = Some(s)
  implicit def toOption(l: Long): Option[Long] = Some(l)

  //formatter to deal with json representations
  implicit val transactionFormat: Format[Transaction] = (
    (__ \ "id").formatNullable[Long] ~
      (__ \ "body").format[String]
    )(Transaction.apply, unlift(Transaction.unapply))


  //DAO stuff
  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](current)
  import dbConfig._
  import dbConfig.driver.api._

  //this class links the DB Table with the case class Transaction
  class TransactionTable(tag: Tag) extends Table[Transaction](tag, "TRANSACTIONS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def body = column[String]("BODY")

    //specify the columns selected as tuple, and map them to a pair of functions to generate a Transaction object, and viceversa
    def * = (id.?, body) <> ((Transaction.apply _).tupled, Transaction.unapply)
  }

  val table = TableQuery[TransactionTable]

  def list: Future[Seq[Transaction]] = {
    val txList = table.result
    //db comes from the dbConfig._
    db.run(txList)
  }

  def findById(id: Long): Future[Option[Transaction]] = {
    val txById = table.filter { tx => tx.id === id}.result.headOption
    db.run(txById)
  }

  def create(tx: Transaction): Future[Transaction] = {
    //we take the table, transform it into the equivalent one that just returns the Tx.id after insertion, and perform the insertion
    //if we run this insertion, we get back the future of this id
    val insertion = (table returning table.map(_.id)) += tx
    val newID = db.run(insertion)

    newID.map(newIdValue => tx.copy(id = newIdValue))
  }
}

