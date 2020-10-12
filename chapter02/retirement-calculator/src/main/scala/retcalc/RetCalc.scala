package retcalc

import scala.annotation.tailrec

object RetCalc {

  def futureCapital(
      returns: Returns,
      nbOfMonths: Int,
      netIncome: Int,
      currentExpenses: Int,
      initialCapital: Double
  ): Double = {
    val monthlySavings = netIncome - currentExpenses
    (0 until nbOfMonths).foldLeft(initialCapital)((accumulated, month) =>
      accumulated * (1 + Returns
        .monthlyRate(returns, month)) + monthlySavings
    )
  }

  def simulatePlan(
      interestRate: Double,
      nbOfMonthsSaving: Int,
      nbOfMonthsInRetirement: Int,
      netIncome: Int,
      currentExpenses: Int,
      initialCapital: Double
  ): (Double, Double) = {
    val capitalAtRetirement = futureCapital(
      returns = FixedReturns(0.04),
      nbOfMonths = nbOfMonthsSaving,
      netIncome = netIncome,
      currentExpenses = currentExpenses,
      initialCapital = initialCapital
    )

    val capitalAfterDeath = futureCapital(
      returns = FixedReturns(0.04),
      nbOfMonths = nbOfMonthsInRetirement,
      netIncome = 0,
      currentExpenses = currentExpenses,
      initialCapital = capitalAtRetirement
    )

    (capitalAtRetirement, capitalAfterDeath)
  }

  def nbOfMonthsSaving(
      interestRate: Double,
      nbOfMonthsInRetirement: Int,
      netIncome: Int,
      currentExpenses: Int,
      initialCapital: Double
  ): Int = {
    @tailrec
    def loop(months: Int): Int = {
      val (_, capitalAfterDeath) = simulatePlan(
        interestRate = interestRate,
        nbOfMonthsSaving = months,
        nbOfMonthsInRetirement = nbOfMonthsInRetirement,
        netIncome = netIncome,
        currentExpenses = currentExpenses,
        initialCapital = initialCapital
      )
      if (capitalAfterDeath > 0.0) months else loop(months + 1)
    }

    if (netIncome > currentExpenses) loop(0) else Int.MaxValue
  }
}
