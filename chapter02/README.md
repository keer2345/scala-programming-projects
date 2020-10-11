# Developing a Retirement Calculator

这一章，我们将接着上一章来实践 Scala 语言特性。我们通过退休计算器来介绍 Scala 语言其他方面以及 SDK 开发逻辑模型。该计算器有助于我们计算想有个舒适的退休生活需要工作多久和存多少钱。

我们通过测试驱动（TDD）来开发，最好先尝试自己编写函数。此外，最好是自己书写代码，而不是复制粘贴。这样将收获更多并更喜欢 IntelliJ 编辑器。

本章主题：
- 计算未来资金
- 计算何时退休
- 使用市场利率
- 打包应用

# 项目概览
使用类似净收入（net income）、开销（expenses）、初始资金（initial capital）等参数，来创建函数进行如下计算：
- 未来的退休金
- 退休若干年后的资金
- 存多久的钱可以退休

我们首先使用固定利率（fixed interest rate）来计算，之后再从 `.tsv` 文件加载市场数据来重构之前的函数以模仿在投资期会发生什么。


# 计算未来资金
当你计划退休时，你首先要知道的是你在你选择的退休日期能得到多少资金。现在，我们假设每月以固定利率投资你的储蓄，简单来说就是忽略通货膨胀的影响。因此资金将以当前的资金计算，利率按 *实际利率 = 名义利率 - 膨胀利率* 计算。

我们不打算在本章体积货币，你可以认为它是是美元、欧元或者其他货币。只要所有的计算都用同一种货币就避讳影响计算结果。

## 为积累期编写测试单元

我们想要的是类似 Excel 中的 `FV` 函数：它根据固定利率计算投资的未来价值。基于 TDD 驱动，我们首先创建失败的测试单元。

1. 重建项目 `retirement-calculator`。
1. 在 `src/main/scala` 和 `src/test/scala` 下面新建包 `retcalc`。
1. 新建 `src/test/scala/RetcalcSpec.scala` 测试单元。
1. 编写代码：

``` scala
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
```

类似第一章，我们使用 `AnyWordSpec` 测试类型，也用 `TypeCheckedTripleEquals` 处理类型，代码提供了强大的断言（assertion）`should ===` 来确保在编译期等式两边类型相同。ScalaTest 默认的断言 `should` 只在运行期校验类型是否相同，我们推荐使用 `should ===`，它将在重构代码时节省很多时间。

另外，允许在比较双精度（double）值时有一定的误差，看看如下声明：

``` scala
        implicit val doubleEquality =
          TolerantNumerics.tolerantDoubleEquality(0.0001)
```
这样在比较 `double1 should ===(double2)` 时允许有小于 `0.0001` 的误差，避免了浮点计算的问题。例如，在 Scala Console 键入如下代码：

``` scala
scala> val double1 = 0.01 - 0.001 + 0.001
val double1: Double = 0.010000000000000002

scala> double1 == 0.01
val res0: Boolean = false
```
不出所料，这是众所周知的在任何语言中都存在的二进制浮点数问题。我们可以用 `BigDecimal` 替代 `Double` 来避免这样的情况，但我们的目标并不需要如此精确，`BigDecimal` 会使计算变得很慢。

这段代码非常直观，我们调用函数并获取期望值。对于重要的计算，我们通常使用 Excel 或者 LibreOffice 来计算，可以使用公式 `FV(0.04/12, 25*12, 1000, 10000, 0)` 来获取。我们假设用户每月将其收入和支出之间的全部差额存起来，那么 `FV` 函数中的 PMT 参数是 `1000 = netIncome - currentExpenses`。

我们现在的测试是失败的，因为 `RetCalc` 对象和 `futureCapital` 函数还不存在。

在 `src/main/scala` 的新包 `retcalc` 中创建对象 `RetCalc`，然后创建 `futureCapital` 函数。

`src/main/scala/retcalc/RetCalc.scala`:

``` scala
package retcalc

object RetCalc {
  def futureCapital(
      interestRate: Double,
      nbOfMonths: Int,
      netIncome: Int,
      currentExpenses: Int,
      initialCapital: Double
  ): Double = ???

}
```

## 实现futureCapital

由于没有实现具体的代码， 测试依旧失败。如果使用 `initialCapital = 10000`，`monthlySavings = 1000` 来计算，我们具体步骤分解如下：
1. 第 0 月，在存款之前，我们的资金为 `capital_0 = initialCapital = 10000`
1. 第 1 月，初始资本增加了利息，还有 1000 的存款。 `capital_1 = capital_0 * (1 + monthlyInterestRate) + 1000`
1. 第 2 月，`capital_2 = capital_1 * (1 + monthlyInterestRate) + 1000`

下面就是函数主体：

``` scala
package retcalc

object RetCalc {
  def futureCapital(
      interestRate: Double,
      nbOfMonths: Int,
      netIncome: Int,
      currentExpenses: Int,
      initialCapital: Double
  ): Double = {
    val monthlySavings = netIncome - currentExpenses

    def nextCapital(accumulated: Double, month: Int): Double =
      accumulated * (1 + interestRate) + monthlySavings

    (0 until nbOfMonths).foldLeft(initialCapital)(nextCapital)
  }

}
```

`foldLeft` 具体是：

``` scala
def foldLeft[z: B](op: (B, A)=>B): B
```

- `[B]` 意思是函数的参数类型为 `B`，当调用该函数时，编译器根据 `z: B` 自动指向 `B`。我们的代码中，`z` 参数为 `initialCapital`，其类型为 `Double`。因此，在 `futureCapital` 中调用 `foldLeft` 来计算，我们定义了 `B = Double`。

``` scala
def foldLeft(z: Double)(op: (Double, A) => Double): Double
```
- 函数有两个参数列表，Scala 允许使用跟多的参数列表。每个列表有一个或多个参数。这不会改变函数的行为；它只是一种分离每个参数列表关注点的方法。
- `op: (B, A) => B` 意思是 `op` 必须为带有类型为 `B` 和 `A` 两个参数的函数，并且返回值类型为 `B`。由于 `foldLeft` 调用另一个函数作为参数，我们称 `foldLeft` 为**高阶函数**（highe order function）。

如果我们将 `coll` 看做集合，`foldLeft` 将是这样的：
1. 创建 `var acc = z`，然后调用 `op` 函数：`acc = op(acc, coll(0))`
1. 捕获集合的每个元素继续调用 `op` 函数：`acc = op(acc, coll(i))`
1. 在遍历集合的所有元素之后返回 `acc`

在 `futureCapital` 函数，我们通过 `op = nextCapital`，`foldLeft` 遍历从 1 到 `nbOfMonths` 的所有 `Int` 类型元素，每次使用前一次的资金来计算金额。注意，到现在我们并没有使用 `nextCapital` 里的 `month` 参数，但我们必须声明它，因为 `foldLeft` 里的 `op` 函数必须要有两个参数。

现在我们再来测试 `RetCalcSpec`，发现已经测试通过了。在 IntelliJ IDEA 或者用 SBT 中测试：

``` scala
> sbt test

...

[info] RetCalcSpec:
[info] RetCalc
[info]   when futureCapital
[info]   - should calculate the amount of savings I will have in n months
[info] Run completed in 778 milliseconds.
[info] Total number of tests run: 1
[info] Suites: completed 1, aborted 0
[info] Tests: succeeded 1, failed 0, canceled 0, ignored 0, pending 0
[info] All tests passed.
[success] Total time: 10 s
```

## 重构生产代码
依据 TDD 方法，测试通过后一般会重构代码。如果测试覆盖率良好，不必担心修改代码，因为任何错误都应该有测试来标记，这就是所谓的**红绿重构**（Red-Green-Refactor）周期。

修改 `futureCapital` 函数的主体：

``` scala
  def futureCapital(
      interestRate: Double,
      nbOfMonths: Int,
      netIncome: Int,
      currentExpenses: Int,
      initialCapital: Double
  ): Double = {
    val monthlySavings = netIncome - currentExpenses

    (0 until nbOfMonths).foldLeft(initialCapital)((accumulated, _) =>
      accumulated * (1 + interestRate) + monthlySavings
    )
  }
```

这里，我们在 `foldLeft` 调用内联的 `nextCapital`。Scala 中我们可以通过这样来定义**匿名函数**：

```
(param1, param2, ..., paramN) => function body
```
可以看到之前 `nextCapital` 的 `month` 参数没有了，在匿名函数中，最佳实践是对无用参数用下划线 （`_`）来表示。参数 `_` 不能在函数中使用，如果尝试用一个名称来代替 `_`，IntelliJ 将会出现下划线的警告，提示该参数是 `Declaration is never used` 的。

## 为聚集期编写测试单元
## 模拟退休计划
### 编写失败的测试单元
### 利用元组
### 实现simulatePlan

# 计算何时退休
# 使用市场利率
# 打包应用 
# 总结
