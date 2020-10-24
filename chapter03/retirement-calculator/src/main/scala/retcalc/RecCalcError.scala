package retcalc

sealed abstract class RetCalcError(val mesage: String)

object RetCalcError {
  case class MoreExpensesThanIncome(income: Double, expenses: Double)
      extends RetCalcError(
        s"Expenses: $expenses >= $income." +
          s" You w ill never be able to save enough to retire !"
      )
}
