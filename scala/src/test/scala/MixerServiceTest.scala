package com.gemini.jobcoin

import java.util.UUID

import api.JobcoinClient
import config.JobcoinConfig
import model.Transaction
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, FlatSpec}
import persistence.AddressMappingStore
import service.{MixerService, TransactionWatcher}

import scala.collection.mutable.ArrayBuffer

class MixerServiceTest extends FlatSpec with BeforeAndAfterEach {

  private val houseAddress = "house_address"
  private val feeAddress = "fee_address"
  private val fee = 1

  private var config: JobcoinConfig = _
  private var client: JobcoinClient = _
  private var transactionWatcher: TransactionWatcher = _
  private var addressMappingStore: AddressMappingStore = _

  var mixerService: MixerService = _

  override protected def beforeEach(): Unit = {
    config = Mockito.mock(classOf[JobcoinConfig])
    initConfig()

    client = Mockito.mock(classOf[JobcoinClient])
    transactionWatcher = Mockito.mock(classOf[TransactionWatcher])
    addressMappingStore = Mockito.mock(classOf[AddressMappingStore])

    mixerService = MixerService(config, client, transactionWatcher, addressMappingStore)
  }

  "start" should "initialize the database connector" in {
    givenAddressStorage(0)

    mixerService.start()

    verify(addressMappingStore, times(1)).loadStore()
  }

  it should "start polling transaction watcher" in {
    givenAddressStorage(0)

    mixerService.start()

    verify(transactionWatcher, times(1)).startPolling()
  }

  it should "register the house address with transaction watcher" in {
    givenAddressStorage(0)

    mixerService.start()

    verify(transactionWatcher, times(1))
      .registerAddress(same(houseAddress))(anyObject[Transaction => Unit]())
  }

  it should "register all stored addresses" in {
    val addressMappings = givenAddressStorage(5)

    mixerService.start()

    assert(mixerService.addressMappings.toArray sameElements addressMappings)
    verify(transactionWatcher, times(addressMappings.length + 1))
      .registerAddress(anyString())(anyObject[Transaction => Unit]())
  }

  "registerAddress" should "add src address to transaction watcher" in {
    val (src, dsts) = (UUID.randomUUID().toString, Array(UUID.randomUUID().toString))

    mixerService.registerAddress(src, dsts, false)

    verify(transactionWatcher, times(1))
      .registerAddress(same(src))(anyObject[Transaction => Unit]())
  }

  it should "add addresses to address mapping" in {
    val (src, dsts) = (UUID.randomUUID().toString, Array(UUID.randomUUID().toString))

    mixerService.registerAddress(src, dsts, false)

    assert(mixerService.addressMappings contains ((src, dsts)))
  }

  private def givenAddressStorage(size: Int): Array[(String, Array[String])] = {
    var mappings: ArrayBuffer[(String, Array[String])] = ArrayBuffer.empty
    if (size > 0) {
      for (i <- 0 until size) {
        val src = UUID.randomUUID().toString
        val dsts = for (j <- 0 until 4) yield UUID.randomUUID().toString
        mappings += src -> dsts.toArray
      }
    }
    when(addressMappingStore.loadStore()).thenReturn(mappings.toArray)
    mappings.toArray
  }


  private def initConfig() = {
    when(config.getHouseAddress).thenReturn(houseAddress)
    when(config.getFeeAddress).thenReturn(feeAddress)
    when(config.getFee).thenReturn(fee)
    when(config.isLoggingEnabled).thenReturn(true)
  }
}
