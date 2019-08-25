package persistence

import config.JobcoinConfig

import scala.reflect.ClassTag

/** Base class to build off of when a class needs to persist a collection of data
  *
  * @param config Jobcoin config
  * @param connector A SqlLiteConnector that's used to store and load data
  * @tparam T Type that will be stored
  */
abstract class Persistable[T](config: JobcoinConfig, connector: SqlLiteConnector[T])(implicit tag: ClassTag[T]) {

  protected var storedData: Array[T] = Array.empty[T]

  protected def initialize(): Unit = {
    connector.connect()
    storedData = connector.loadStore()
  }

  def persist(data: T): Unit = {
    connector.saveData(data)
  }
}
