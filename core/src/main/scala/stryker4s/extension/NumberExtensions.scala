package stryker4s.extension

object NumberExtensions {

  implicit class DoubleRoundTwoDecimals(val score: Double) extends AnyVal {
    def roundDecimals(decimals: Int): Double =
      if (!score.isNaN())
        BigDecimal(score).setScale(decimals, BigDecimal.RoundingMode.HALF_UP).toDouble
      else
        score
  }

}
