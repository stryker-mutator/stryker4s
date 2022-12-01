package stryker4jvm.extensions

object NumberExtensions {

  implicit final class RoundDecimalsExtension(val score: Double) extends AnyVal {
    final def roundDecimals(decimals: Int): Double =
      if (!score.isNaN())
        BigDecimal(score).setScale(decimals, BigDecimal.RoundingMode.HALF_UP).toDouble
      else
        score
  }

}
