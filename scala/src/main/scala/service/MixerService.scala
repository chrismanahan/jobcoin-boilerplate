package service

import api.JobcoinClient
import config.JobcoinConfig
import model.Transaction
import persistence.{AddressMappingStore, Persistable}
import util.MoneyUtil

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.Random

/**
  *
  * @param config
  * @param client
  * @param transactionWatcher
  * @param connector
  */
class MixerService(config: JobcoinConfig,
                   client: JobcoinClient,
                   transactionWatcher: TransactionWatcher,
                   connector: AddressMappingStore) extends Persistable[(String, Array[String])](config, connector) {

  var addressMappings: ArrayBuffer[(String, Array[String])] = ArrayBuffer.empty

  /** Starts the mixer service by loading data stores and starting the TransactionWatcher to poll for transactions
    *
    */
  def start(): Unit = {
    initialize()
    transactionWatcher.initialize()

    storedData.foreach { case(sourceAddress, destinations) =>
      registerAddress(sourceAddress, destinations, shouldPersist = false)
    }

    transactionWatcher.registerAddress(config.getHouseAddress)(distributeFromHouse)

    transactionWatcher.startPolling()
  }

  /** Registers an address to watch transactions for and sends random amounts to destinationAddresses when money
    * is sent to sourceAddress
    *
    * @param sourceAddress Address to watch transactions from
    * @param destinationAddresses Address to distribute amount from observed transaction to
    * @param shouldPersist Flag indicating if we should persist the address mapping
    */
  def registerAddress(sourceAddress: String, destinationAddresses: Array[String], shouldPersist: Boolean = true): Unit = {
    if (config.isLoggingEnabled) println(s"> registering $sourceAddress.")
    if (config.isLoggingEnabled) println(s">> with destinations: ${destinationAddresses.reduce(_+","+_)}")
    val address = sourceAddress
    val entry = address -> destinationAddresses
    addressMappings += entry
    if (shouldPersist) persist(entry)

    transactionWatcher.registerAddress(address) { transaction =>
      println(s"> got transaction for address: $address")
      if (isValidTransaction(transaction, address)) {
        triggerSend(address, transaction.amount)
      } else {
        println(s"invalid transaction: ${transaction.toString}")
        returnToSender(transaction)
      }
    }
  }

  private def triggerSend(sourceAddress: String, amount: String): Future[Unit] = {
    println(s"> triggering send from $sourceAddress of amt $amount ")

    // split amount for each address
    val (remainingAmount, fee) = splitOutFee(amount)

    val mixerAmounts = Array(
      config.getHouseAddress -> remainingAmount,
        config.getFeeAddress -> fee
    )

    queueScatteredSends(sourceAddress, mixerAmounts)
  }

  private def queueScatteredSends(sourceAddress: String, destinations: Array[(String, String)]): Future[Unit] = {
    println(s"> queueing scattered sends from $sourceAddress")
    destinations.foreach { case(address, amount) => println(s"\t$amount -> $address")}
      Future {
        for ((address, amt) <- destinations) {
          val sleepTime = Random.nextInt(config.getMaxWaitTimeSeconds)
          Thread.sleep(sleepTime * 1000)
          println(s"\tsending $amt to $address")
          client.sendAmount(amt.toString, address, sourceAddress) { (success, errorMessage) =>
            println(errorMessage)
          }
        }
    }
  }

  private def queueScatteredSends(sourceAddress: String, amount: String, destinationAddresses: Array[String]): Future[Unit] = {
    val amounts = splitAmount(amount.toInt, destinationAddresses.length).map(_.toString)
    val shuffledDestinations = Random.shuffle(destinationAddresses.toList).toArray
    queueScatteredSends(sourceAddress, shuffledDestinations.zip(amounts))
  }

  private def splitAmount(amount: Int, times: Int): Array[Int] = {
    var amounts: ArrayBuffer[Int] = ArrayBuffer.empty
    println(s"splitting $amount $times times")
    for (i <- 0 until times) {
      var amt = Random.nextInt(amount / times)
      amounts += amt
    }
    var difference = amount - amounts.sum
    if (difference > 0) {
      // append whatevers left to random amounts
      while (difference > 0) {
        val index = Random.nextInt(amounts.length)
        val amt = if (difference == 1) 1 else Random.nextInt(difference)
        amounts(index) += amt
        difference -= amt
      }
    }

    amounts.toArray
  }

  private def returnToSender(transaction: Transaction): Unit = {
    println(s"returning tx to sender: ${transaction.toString}")
    client.sendAmount(transaction.amount, transaction.fromAddress.get, transaction.toAddress) { (success, msg) =>
      if (success) println(s"> returned successfully: ${transaction.toString}")
      else println(msg)
    }
  }

  private def isValidTransaction(tx: Transaction, address: String): Boolean = {
    getDestinations(address) match {
      case Some(destinations) => {
        val amt = MoneyUtil.getIntegerDigits(tx.amount)
        (amt - config.getFee) >= destinations.length
      }
      case None => {
        val dsts = getDestinations(tx.fromAddress.getOrElse(""))
        if (tx.toAddress == config.getHouseAddress && dsts.isDefined) {
          tx.amount.toInt >= dsts.get.length
        } else false
      }
    }
  }

  private def distributeFromHouse(transaction: Transaction) = {
    assert(transaction.toAddress == config.getHouseAddress)
    if (config.isLoggingEnabled) println("> distributing from house")

    if (isValidTransaction(transaction, config.getHouseAddress)) {
      getDestinations(transaction.fromAddress.get) match {
        case Some(destinations) => queueScatteredSends(transaction.toAddress, transaction.amount, destinations)
        case None =>
      }
    }
  }

  private def getDestinations(address: String): Option[Array[String]] = {
    val dsts = addressMappings.toMap.get(address)
    if (dsts.isDefined && config.isLoggingEnabled) {
      println(s"> got destinations for $address: ${dsts.get.reduce(_+","+_)}")
    }

    dsts
  }

  /**
    * Splits an amount into the fee to be taken and remaining amount
    * @param amount Initial amount
    * @return (leftoverAmount, feeAmount)
    */
  private def splitOutFee(amount: String): (String, String) = {
    val intAmount = MoneyUtil.getIntegerDigits(amount)
    val leftover = (intAmount - config.getFee).toString
    val feeAmount = config.getFee.toString + MoneyUtil.getMantissa(amount)
    (leftover, feeAmount)
  }
}

object MixerService {
  def apply(config: JobcoinConfig, client: JobcoinClient, transactionWatcher: TransactionWatcher, connector: AddressMappingStore): MixerService = {
    new MixerService(config, client, transactionWatcher, connector)
  }
}