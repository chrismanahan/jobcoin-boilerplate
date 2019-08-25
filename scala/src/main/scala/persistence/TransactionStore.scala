package persistence

import java.sql.ResultSet

import config.JobcoinConfig
import model.Transaction

import scala.collection.mutable.ArrayBuffer

class TransactionStore(config: JobcoinConfig) extends SqlLiteConnector[Transaction](config) {

  private val tableName = "Transactions"
  private val colTimestamp = "timestamp"
  private val colToAddress = "toAddress"
  private val colFromAddress = "fromAddress"
  private val colAmount = "amount"

  override val createTableQuery: String = s"CREATE TABLE IF NOT EXISTS $tableName " +
    s"($colTimestamp string, $colToAddress string, " +
    s"$colFromAddress string, $colAmount string)"

  /** Inserts transaction into db
    *
    * @param transaction Transaction to be saved
    * @return
    */
  def insertTransaction(transaction: Transaction): Int = {
    // doing `ignore` to prevent duplicate transactions. hacky fix to bug where transactions with very low (i.e: 1.0E-38)
    // amount are being duplicated somewhere
    val query = s"INSERT OR IGNORE INTO $tableName VALUES(${sqlValuesFromTransaction(transaction)})"
    executeUpdate(query)
  }

  /** Fetches all stored transactions
    *
    * @return All stored transactions
    */
  def fetchTransactions(): Array[Transaction] = {
    val query = s"SELECT * FROM $tableName"
    val transactions = executeQuery(query) match {
      case Some(results) => Some(transactionsFromResults(results))
      case None => None
    }

    transactions.getOrElse(Array.empty)
  }

  override def loadStore(): Array[Transaction] = fetchTransactions()

  override def saveData(data: Transaction): Unit = insertTransaction(data)

  private def sqlValuesFromTransaction(transaction: Transaction): String = {
    val fromAddress = transaction.fromAddress.getOrElse("")
    s"'${transaction.timestamp}', '${transaction.toAddress}', '$fromAddress', '${transaction.amount}'"
  }

  private def transactionsFromResults(results: ResultSet): Array[Transaction] = {
    var transactions: ArrayBuffer[Transaction] = ArrayBuffer.empty
    while (results.next()) {
      val tx = Transaction(
        results.getString(colTimestamp),
        results.getString(colToAddress),
        if (results.getString(colFromAddress) == "") None else Some(results.getString(colFromAddress)),
        results.getString(colAmount)
      )
      transactions += tx
    }
    transactions.toArray
  }
}

object TransactionStore {
  def apply(config: JobcoinConfig): TransactionStore = new TransactionStore(config)
}