package retcalc

import org.scalactic.{Equality, TolerantNumerics, TypeCheckedTripleEquals}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ReturnsSpec
    extends AnyWordSpec
    with Matchers
    with TypeCheckedTripleEquals {

  implicit val doubleEquality: Equality[Double] =
    TolerantNumerics.tolerantDoubleEquality(0.0001)

  "VariableReturns" when {
    "formUntil" should {
      "keep only a window of the returns" in {
        val variableReturns = VariableReturns(Vector.tabulate(12) { i =>
          val d = (i + 1).toDouble
          VariableReturn(f"2017.$d%02.0f", d)
        })

        variableReturns should ===(
          VariableReturns(
            Vector(
              VariableReturn("2017.01", 1.0),
              VariableReturn("2017.02", 2.0),
              VariableReturn("2017.03", 3.0),
              VariableReturn("2017.04", 4.0),
              VariableReturn("2017.05", 5.0),
              VariableReturn("2017.06", 6.0),
              VariableReturn("2017.07", 7.0),
              VariableReturn("2017.08", 8.0),
              VariableReturn("2017.09", 9.0),
              VariableReturn("2017.10", 10.0),
              VariableReturn("2017.11", 11.0),
              VariableReturn("2017.12", 12.0)
            )
          )
        )

        variableReturns.fromUtils("2017.07", "2017.09").returns should ===(
          Vector(
            VariableReturn("2017.07", 7.0),
            VariableReturn("2017.08", 8.0)
          )
        )

        variableReturns.fromUtils("2017.10", "2018.01").returns should ===(
          Vector(
            VariableReturn("2017.10", 10.0),
            VariableReturn("2017.11", 11.0),
            VariableReturn("2017.12", 12.0)
          )
        )

        val variableReturns2 = VariableReturns(
          Vector(
            VariableReturn("2017.09", 9.0),
            VariableReturn("2017.10", 10.0),
            VariableReturn("2017.11", 11.0),
            VariableReturn("2017.12", 12.0),
            VariableReturn("2018.01", 1.0),
            VariableReturn("2018.02", 2.0),
            VariableReturn("2018.03", 3.0)
          )
        )
        variableReturns2.fromUtils("2017.10", "2018.02").returns should ===(
          Vector(
            VariableReturn("2017.10", 10.0),
            VariableReturn("2017.11", 11.0),
            VariableReturn("2017.12", 12.0),
            VariableReturn("2018.01", 1.0)
          )
        )
      }
    }
  }
}
