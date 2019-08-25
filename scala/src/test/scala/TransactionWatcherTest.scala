import java.util.UUID

import api.JobcoinClient
import config.JobcoinConfig
import model.Transaction
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.{BeforeAndAfterEach, FlatSpec}
import persistence.TransactionStore
import service.TransactionWatcher

import scala.collection.mutable.ArrayBuffer

class TransactionWatcherTest extends FlatSpec with BeforeAndAfterEach{

  private var config: JobcoinConfig = Mockito.mock(classOf[JobcoinConfig])
  private var client: JobcoinClient = Mockito.mock(classOf[JobcoinClient])
  private var transactionStore: TransactionStore = Mockito.mock(classOf[TransactionStore])

  private var transactionWatcher: TransactionWatcher = _

  override protected def beforeEach(): Unit = {
    config = Mockito.mock(classOf[JobcoinConfig])
    client = Mockito.mock(classOf[JobcoinClient])
    transactionStore = Mockito.mock(classOf[TransactionStore])

    transactionWatcher = TransactionWatcher(client, config, transactionStore)
  }

  "initialize" should "load stored transactions" in {
    val store = givenTransactionStorage(10)

    transactionWatcher.initialize()

    assert(transactionWatcher.allTransactions sameElements store)
  }

  "registerAddress" should "add address and callback to subscribers" in {
    val addy = UUID.randomUUID().toString

    transactionWatcher.registerAddress(addy)((t: Transaction)=> _)

    assert(transactionWatcher.subscribers(addy) != null)
  }

  "startPolling" should "call poll" in {
    transactionWatcher.startPolling()
  }

  private def givenTransactionStorage(size: Int): Array[Transaction] = {
    var transactions: ArrayBuffer[Transaction] = ArrayBuffer.empty
    if (size > 0) {
      for (i <- 0 until size) {
        val tx = Transaction("time", UUID.randomUUID().toString,
                              Some(UUID.randomUUID().toString), "10")
        transactions += tx
      }
    }
    when(transactionStore.loadStore()).thenReturn(transactions.toArray)
    transactions.toArray
  }
}
