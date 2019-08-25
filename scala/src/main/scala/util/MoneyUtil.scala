package util

object MoneyUtil {

  def getIntegerDigits(amount: String): Int = {
    try {
      amount.toInt
    } catch {
      case _: NumberFormatException => amount.split("\\.").head.toInt
    }
  }

  def getMantissa(amount: String): String = {
    try {
      if (amount.contains(".")) amount.split(".").reverse.head
      else ""
    } catch {
      case e: Throwable => ""
    }
  }
}
