package service

import api.JobcoinClient
import config.JobcoinConfig
import model.Transaction
import persistence.{Persistable, TransactionStore}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class TransactionWatcher(client: JobcoinClient, config: JobcoinConfig, connector: TransactionStore) extends Persistable[Transaction](config, connector) {

  var subscribers: Map[String, Transaction => Unit] = Map.empty

  private var isPolling = false

  var allTransactions: Array[Transaction] = Array.empty[Transaction]

  private var delayS = 5

  @Override
  override def initialize(): Unit = {
    super.initialize()
    allTransactions = storedData
  }

  /**
    * Registers a callback to a given address. callback is called when a new transaction occurs involving
    * the registered address
    *
    * @param address
    * @param callback
    */
  def registerAddress(address: String)(callback: Transaction => Unit): Unit = {
    subscribers += (address -> callback)
  }

  def startPolling(): Unit = {
    if (!isPolling) {
      if (config.isLoggingEnabled) println("start polling")
      poll()
      isPolling = true
    }
  }

  private def poll(): Unit = {
    Future {
      client.getAllTransactions() { transactions =>
        if (transactions.length > getAllTransactions.length) {
          // new transactions available
          val newTransactions = findNewElements(transactions, getAllTransactions)
            .filter(tx => !storedData.contains(tx))
          newTransactions
            .foreach(persist)

          if (config.isLoggingEnabled) println(s"${newTransactions.length} new transactions")
          allTransactions = transactions.distinct

          // find subscribed subscribers
          newTransactions.foreach { tx =>
            findSubscriber(subscribers, tx) match {
              case Some(address) => subscribers(address)(tx)
              case None =>
            }
          }
        }

        Thread.sleep(delayS * 1000)
        poll();
      }
    }
  }

  private def getAllTransactions: Array[Transaction] = allTransactions.distinct

  private def findSubscriber[T](subscribers: Map[String, T], transaction: Transaction): Option[String] = {
    if (getSubscriberAddresses.contains(transaction.toAddress)) {
      if (config.isLoggingEnabled) println(s"got subscriber for ${transaction.toString}")
      return Some(transaction.toAddress)
    }
    return None
  }

  private def getSubscriberAddresses: Array[String] = {
    subscribers.keys.toArray
  }

  private def findNewElements[T](newArray: Array[T], existingArray: Array[T]): Array[T] = {
    newArray.filter { e => !existingArray.contains(e) }
  }
}

object TransactionWatcher {
  def apply(client: JobcoinClient, config: JobcoinConfig, connector: TransactionStore): TransactionWatcher = {
    new TransactionWatcher(client, config, connector)
  }
}