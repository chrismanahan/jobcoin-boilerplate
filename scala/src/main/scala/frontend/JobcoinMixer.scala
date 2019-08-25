package frontend

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import api.JobcoinClient
import com.typesafe.config.ConfigFactory
import service.{MixerService, TransactionWatcher}
import config.JobcoinConfig
import persistence.{AddressMappingStore, TransactionStore}

import scala.io.StdIn

object JobcoinMixer {
  object CompletedException extends Exception { }

  def main(args: Array[String]): Unit = {
    // Create an actor system
    implicit val actorSystem = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val jobcoinConfig = JobcoinConfig(ConfigFactory.load())
    val client = JobcoinClient(jobcoinConfig)
    val transactionWatcher = TransactionWatcher(client, jobcoinConfig, TransactionStore(jobcoinConfig))
    val mixerService = MixerService(jobcoinConfig, client, transactionWatcher, AddressMappingStore(jobcoinConfig))
    mixerService.start()

    try {
      while (true) {
        println(prompt)
        val line = StdIn.readLine()

        if (line == "quit") throw CompletedException

        val addresses = line.split(",")
        if (line == "") {
          println(s"You must specify empty subscribers to mix into!\n$helpText")
        } else {
          val depositAddress = UUID.randomUUID().toString

          mixerService.registerAddress(depositAddress, addresses)

          println(s"You may now send Jobcoins to address $depositAddress. They will be mixed and sent to your destination subscribers.")
        }
      }
    } catch {
      case CompletedException => println("Quitting...")
    } finally {
      actorSystem.terminate()
    }
  }

  val prompt: String = "Please enter a comma-separated list of new, unused Jobcoin subscribers where your mixed Jobcoins will be sent.\n" +
    "Use of this service will cost 1 Jobcoin. Any fractional amounts will be taken as additional service charge\n" +
    "* You must send more Jobcoins than addresses. For example, sending 5 coins to a total of 6 addresses will be rejected"
  val helpText: String =
    """
      |Jobcoin Mixer
      |
      |Takes in at least one return address as parameters (where to send coins after mixing). Returns a deposit address to send coins to.
      |
      |Usage:
      |    run return_addresses...
    """.stripMargin
}
