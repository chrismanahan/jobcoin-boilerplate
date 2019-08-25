package persistence

import java.sql.ResultSet

import config.JobcoinConfig

class AddressMappingStore(config: JobcoinConfig) extends SqlLiteConnector[(String, Array[String])](config) {

  private val tableName = "AddressMappings"
  private val colSrc = "srcAddress"
  private val colDst = "dstAddress"

  override val createTableQuery: String = s"CREATE TABLE IF NOT EXISTS $tableName ($colSrc string, $colDst string)"

  /** Inserts an address mapping into datastore
    */
  def insertAddressMappings(sourceAddress: String, destinations: Array[String]): Unit = {
    println(s">> Inserting address mapping into sql: $sourceAddress, ${destinations.length} dests")
    for (dst <- destinations) {
      val query = s"INSERT INTO $tableName VALUES('$sourceAddress', '$dst')"
      executeUpdate(query)
    }
  }

  /** Fetches all stored address mappings
    *
    * @return
    */
  def fetchAddressMappings(): Array[(String, Array[String])] = {
    val query = s"SELECT $colSrc, $colDst FROM $tableName"
    val mapping = executeQuery(query) match {
      case Some(results) => Some(buildMapFromResults(results))
      case None => None
    }

    mapping.getOrElse(Map.empty).toArray
  }

  override def loadStore(): Array[(String, Array[String])] = fetchAddressMappings()

  override def saveData(data: (String, Array[String])): Unit = insertAddressMappings(data._1, data._2)

  private def buildMapFromResults(results: ResultSet): Map[String, Array[String]] = {
    var mapping: Map[String, Array[String]] = Map.empty
    while (results.next()) {
      val key = results.getString(colSrc)
      if (mapping.contains(key)) {
        val value = mapping(key) ++ Array(results.getString(colDst))
        mapping += (key -> value)
      } else {
        mapping += (key -> Array(results.getString(colDst)))
      }
    }
    mapping
  }
}

object AddressMappingStore {
  def apply(config: JobcoinConfig): AddressMappingStore = new AddressMappingStore(config)
}