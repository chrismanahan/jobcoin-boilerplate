package model

import akka.util.ByteString
import play.api.libs.json.{JsPath, Json, Writes}
import play.api.libs.ws.{BodyWritable, InMemoryBody}
import play.api.libs.functional.syntax._


case class NewTransaction(toAddress: String, fromAddress: String, amount: String)
object NewTransaction {
  implicit val jsonWrites: Writes[NewTransaction] = (
    (JsPath \ "toAddress").write[String] and
      (JsPath \ "fromAddress").write[String] and
      (JsPath \ "amount").write[String]
    )(unlift(NewTransaction.unapply))

  implicit val bodyWritable: BodyWritable[NewTransaction] =
    BodyWritable(obj => InMemoryBody(ByteString.fromString(Json.stringify(Json.toJson(obj)))), "application/json")
}