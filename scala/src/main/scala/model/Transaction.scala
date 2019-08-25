package model

import play.api.libs.json.{Json, Reads}

case class Transaction(timestamp: String, toAddress: String, fromAddress: Option[String], amount: String)
object Transaction {
  implicit val jsonReads: Reads[Transaction] = Json.reads[Transaction]
}