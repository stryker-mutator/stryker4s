package stryker4s.config

import stryker4s.extension.exception.InvalidThresholdValueException

case class Thresholds(high: Int = 80, low: Int = 60, break: Int = 0) {
  failIfNotPercentage(high, low, break)

  if (high < low)
    throw InvalidThresholdValueException(s"'high'($high) must be greater than or equal to 'low'($low).")
  if (low <= break)
    throw InvalidThresholdValueException(s"'low'($low) must be greater than 'break'($break).")

  private def failIfNotPercentage(values: Int*): Unit = {
    values.foreach(
      value =>
        if (value < 0 || value > 100)
          throw InvalidThresholdValueException(s"Threshold values must be 0-100. Current: $value."))
  }
}
