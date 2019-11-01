package stryker4s.extension.score

trait MutationScoreCalculator {
  def calculateMutationScore(totalMutants: Double, detectedMutants: Double): Double = {
    detectedMutants / totalMutants * 100 match {
      case mutationScore if mutationScore.isNaN =>
        0.00
      case mutationScore =>
        BigDecimal(mutationScore).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
    }
  }
}
