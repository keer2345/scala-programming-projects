package retcalc

sealed trait Returns
case class FixedReturns(annualRate: Double) extends Returns
case class VariableReturns(returns: Vector[VariableReturn]) extends Returns {
  def fromUtils(monthIdFrom: String, monthIdUntil: String): VariableReturns =
    VariableReturns(
      returns
        .dropWhile(_.monthId != monthIdFrom)
        .takeWhile(_.monthId != monthIdUntil)
    )
}

case class VariableReturn(monthId: String, monthlyRate: Double)

object Returns {
  def monthlyRate(returns: Returns, month: Int): Double =
    returns match {
      case FixedReturns(r) => r / 12
    }
}
