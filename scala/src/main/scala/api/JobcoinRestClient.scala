package api

import akka.stream.Materializer
import api.JobcoinRestClient.AddressResponse
import config.JobcoinConfig
import model.{NewTransaction, Transaction}
import play.api.libs.json._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.ahc._

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class JobcoinRestClient(config: JobcoinConfig)(implicit materializer: Materializer) {
  private val wsClient = StandaloneAhcWSClient()
  private val apiAddressesUrl = config.getApiAddressesUrl
  private val apiTransactionUrl = config.getApiTransactionsUrl
  
  // Docs:
  // https://github.com/playframework/play-ws
  // https://www.playframework.com/documentation/2.6.x/ScalaJsonCombinators
  def getAddress(address: String): Future[AddressResponse] = async {
    val response = await {
      wsClient
        .url(apiAddressesUrl + address)
        .get()
    }

    response
      .body[JsValue]
      .validate[AddressResponse]
      .get
  }

  def getTrasactions(): Future[Array[Transaction]] = async {
    val url = apiTransactionUrl
    val response = await {
      wsClient
        .url(url)
        .get()
    }

    response
      .body[JsValue]
      .validate[Array[Transaction]]
      .get
  }

  def postTransaction(tx: NewTransaction): Future[(String, String)] = async {

    val response = await {
      wsClient
        .url(apiTransactionUrl)
        .post(tx)
    }

    response
      .body[JsValue]
      .validate[(String, String)]
      .get
  }
}

object JobcoinRestClient {

  def apply(config: JobcoinConfig)(implicit materializer: Materializer): JobcoinRestClient = new JobcoinRestClient(config)

  case class AddressResponse(balance: String, transactions: Array[Transaction])
  object AddressResponse {
    implicit val jsonReads: Reads[AddressResponse] = Json.reads[AddressResponse]
  }

  case class TransactionResponse(transactions: Array[Transaction])
  object TransactionResponse {
    implicit val jsonReads: Reads[TransactionResponse] = Json.reads[TransactionResponse]
  }

  // TODO: this class not coming back with response
  case class PostedTransactionResponse(status: Option[String], error: Option[String])
  object PostedTransactionResponse {
    implicit val jsonReads: Reads[PostedTransactionResponse] = Json.reads[PostedTransactionResponse]
  }

}
