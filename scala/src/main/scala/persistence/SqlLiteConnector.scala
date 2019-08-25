package persistence

import java.sql.{Connection, DriverManager, ResultSet, Statement}

import config.JobcoinConfig

/** Abstract class to connect to SQL lite store
  */
abstract class SqlLiteConnector[T](config: JobcoinConfig) {

  // Override with create table query. Should be a `CREATE TABLE IF NOT EXISTS` statement
  val createTableQuery: String

  /** Connect and create table
    *
    * @return
    */
  def connect() = {
    // load driver
    Class.forName(config.getDbDefaultDriver)
    // create table
    try {
      executeUpdate(createTableQuery)
    } catch {
      case (e) => println(s"exception creating table: $e")
    }
  }

  /** Implement a fetcher that loads the datastore into memory
    *
    * @return Stored data
    */
  def loadStore(): Array[T] = ???

  /** Implement method to persist data
    *
    * @param data Data to be persisted
    */
  def saveData(data: T): Unit = ???

  private def getConnection: Connection = DriverManager.getConnection(config.getDbDefaultUrl)

  private def newStatement(): Statement = {
    val statement = getConnection.createStatement()
    statement.setQueryTimeout(30)
    statement
  }

  protected def executeUpdate(query: String): Int = {
    try {
      newStatement().executeUpdate(query)
    } catch {
      case e: Throwable => {
        println(s"exception executing update: $e")
        0
      }
    }
  }

  protected def executeQuery(query: String): Option[ResultSet] = {
    try {
      Some(newStatement().executeQuery(query))
    } catch {
      case e: Throwable => {
        println(s"exception executing query: $e")
        None
      }
    }
  }
}