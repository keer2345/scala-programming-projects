package retcalc

import org.scalactic.{TolerantNumerics, TypeCheckedTripleEquals}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RetCalcSpec
    extends AnyWordSpec
    with Matchers
    with TypeCheckedTripleEquals {
  "RetCalc" when {
    "futureCapital" should {
      "calculate the amount of savings I will have in n months" in {
        implicit val doubleEquality =
          TolerantNumerics.tolerantDoubleEquality(0.0001)
        val actual =
          RetCalc.futureCapital(
            interestRate = 0.04 / 12,
            nbOfMonths = 25 * 12,
            netIncome = 3000,
            currentExpenses = 2000,
            initialCapital = 10000
          )
        val expected = 541267.199
        actual should ===(expected)
      }
    }
  }
}
