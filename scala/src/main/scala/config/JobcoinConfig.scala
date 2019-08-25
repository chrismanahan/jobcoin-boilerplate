package config

import com.typesafe.config.Config

class JobcoinConfig(config: Config) {
  def getApiBaseUrl: String = getJobcoin("apiBaseUrl")
  def getApiAddressesUrl: String = getJobcoin("apiAddressesUrl")
  def getApiTransactionsUrl: String = getJobcoin("apiTransactionsUrl")
  def getFee: Int = getJobcoin("fee").toInt
  def getFeeAddress: String = getJobcoin("feeAddress")
  def getHouseAddress: String = getJobcoin("houseAddress")
  def getMaxWaitTimeSeconds: Int = getJobcoin("maxWaitTimeSeconds").toInt
  def getDbConfig: String = getPlayDb("db")
  def getDbDefault: String = getPlayDb("default")
  def getDbDefaultDriver: String = getDbDefault("driver")
  def getDbDefaultUrl: String = getDbDefault("url")
  def isLoggingEnabled: Boolean = getJobcoin("logging").toBoolean

  private def getJobcoin(path: String): String = getPath("jobcoin", path)
  private def getPlayDb(path: String): String = getPath("play.db", path)
  private def getDbDefault(path: String): String = getPath("db.default", path)
  private def getPath(prefix: String, path: String) = config.getString(s"$prefix.$path")
}

object JobcoinConfig {
  def apply(config: Config): JobcoinConfig = new JobcoinConfig(config)
}