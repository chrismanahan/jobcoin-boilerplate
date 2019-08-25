package api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import config.JobcoinConfig
import model.{NewTransaction, Transaction}

import scala.concurrent.ExecutionContext.Implicits.global

class JobcoinClient(config: JobcoinConfig) {
  implicit private val actorSystem = ActorSystem()
  implicit private val materializer = ActorMaterializer()
  private val restClient = JobcoinRestClient(config)

  def getAddress(address: String)(completion: (String, Array[Transaction]) => Unit): Unit = {
    restClient.getAddress(address).foreach { resp =>
      completion(resp.balance, resp.transactions)
    }
  }

  def getAllTransactions()(completion: (Array[Transaction] => Unit)): Unit = {
    restClient.getTrasactions().foreach { transactions =>
      completion(transactions)
    }
  }

  def getAllAddresses()(completion: (Array[String] => Unit)): Unit = {
    getAllTransactions() { txs =>
      txs.map(tx => tx.toAddress).distinct
    }
  }

  def sendAmount(amount: String, toAddress: String, fromAddress: String)(completion: (Boolean, Option[String]) => Unit): Unit = {
    val tx = NewTransaction(toAddress, fromAddress, amount)

    restClient.postTransaction(tx).foreach { resp =>
      if (resp._1 == "error") {
        completion(false, Some(resp._2))
      } else {
        completion(true, None)
      }
    }
  }
}

object JobcoinClient {
  def apply(config: JobcoinConfig): JobcoinClient = new JobcoinClient(config)
}