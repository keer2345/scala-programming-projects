# Developing a Retirement Calculator

<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-refresh-toc -->
**Table of Contents**

- [Developing a Retirement Calculator](#developing-a-retirement-calculator)
- [项目概览](#项目概览)
- [计算未来资金](#计算未来资金)
    - [为积累期编写测试单元](#为积累期编写测试单元)
    - [实现futureCapital](#实现futurecapital)
    - [重构生产代码](#重构生产代码)
    - [进一步编写测试单元](#进一步编写测试单元)
    - [模拟退休计划](#模拟退休计划)
        - [编写失败的测试单元](#编写失败的测试单元)
        - [利用元组](#利用元组)
        - [实现simulatePlan](#实现simulateplan)
- [计算何时退休](#计算何时退休)
    - [测试失败的nbOfMonthsSaving](#测试失败的nbofmonthssaving)
    - [编写函数体](#编写函数体)
    - [理解尾部递归](#理解尾部递归)
    - [确保终止](#确保终止)
- [使用市场利率](#使用市场利率)
    - [定义代数数据类型](#定义代数数据类型)
    - [筛选特定期间的回报](#筛选特定期间的回报)
    - [模式匹配](#模式匹配)
    - [重构simulatePlan](#重构simulateplan)
    - [加载市场数据](#加载市场数据)
        - [构建测试单元](#构建测试单元)
        - [从文件加载数据](#从文件加载数据)
        - [加载通货膨胀数据](#加载通货膨胀数据)
    - [计算真实回报](#计算真实回报)
- [打包应用](#打包应用)
    - [创建应用项目](#创建应用项目)
    - [打包应用](#打包应用-1)
- [总结](#总结)

<!-- markdown-toc end -->


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

## 进一步编写测试单元

现在我们已经知道在退休时有多少资金，是时候再次用 `futureCapital` 来计算能有多少资金留给继承人了。

在 `RetCalcSpec` 添加测试：

``` scala
class RetCalcSpec
    extends AnyWordSpec
    with Matchers
    with TypeCheckedTripleEquals {
  "RetCalc" when {
    "futureCapital" should {

      implicit val doubleEquality =
        TolerantNumerics.tolerantDoubleEquality(0.0001)

      "calculate the amount of savings I will have in n months" in {
        // ...
      }

      "calculate how much savings will be left after having taken a pension for n months" in {
        val actual =
          RetCalc.futureCapital(
            interestRate = 0.04 / 12,
            nbOfMonths = 40 * 12,
            netIncome = 0,
            currentExpenses = 2000,
            initialCapital = 541267.1990
          )
        val expected = 309867.53176
        actual should ===(expected)
      }
    }
  }
}
```

所以，我们在退休的40年后，每个月开销一样并且没有任何收入的话，将有一笔客观的资金类给继承人。如果预留的资金是负数，意味着花光了继续，这是我们需要避免的。

可以从 Scala Console 调用函数来尝试不同的值，以更加匹配个人的情况，通过不同的利率观察资金变负数的情况。

请注意，在生产情况下，你将添加更多的测试来覆盖边界情况，以确保函数不会崩溃。就像我们[第三章](https://github.com/keer2345/scala-programming-projects/tree/main/chapter03)将要说到的*错误处理*，我们可以假设 `futureCapital` 的测试覆盖已足够 优秀。

## 模拟退休计划

目前已经知道如何计算退休和死亡时的资金，将两个调用绑定到一个简单的函数会更好，此函数将一次性模拟退休计划。

### 编写失败的测试单元

`RetCalcSpec`:
``` scala
class RetCalcSpec
    extends AnyWordSpec
    with Matchers
    with TypeCheckedTripleEquals {

  implicit val doubleEquality =
    TolerantNumerics.tolerantDoubleEquality(0.0001)

  "RetCalc" when {
  
    // ...

    "simulatePlan" should {
      "calculate the capital at retirement and capital after death" in {
        val (capitalAtRetirement, capitalAfterDeath) = RetCalc.simulatePlan(
          interestRate = 0.04 / 12,
          nbOfMonthsSaving = 25 * 12,
          nbOfMonthsInRetirement = 40 * 12,
          netIncome = 3000,
          currentExpenses = 2000,
          initialCapital = 10000
        )
        capitalAtRetirement should ===(541267.1990)
        capitalAfterDeath should ===(309867.5316)
      }
    }
  }
}
```
`RetCalc.scala`:
``` scala
  def simulatePlan(
      interestRate: Double,
      nbOfMonthsSaving: Int,
      nbOfMonthsInRetirement: Int,
      netIncome: Int,
      currentExpenses: Int,
      initialCapital: Double
  ): (Double, Double) = ???
```

### 利用元组

这时候测试是失败的，`simulatePlan` 函数必须返回两个值，最简单的方式是返回 `Tuple2`。Scala 中元组是不可变的数据结构并支持多种不同类型的对象，这些对象在元组中是固定的。就像属性没有特定名称的 `case class`。在类型论中，我们称元组或 `case class` 是**生产类型**（product type）。

``` scala
scala> val tuple3 = (1, "hello", 2.0)
val tuple3: (Int, String, Double) = (1,hello,2.0)

scala> tuple3._1
val res0: Int = 1

scala> tuple3._2
val res1: String = hello

scala> val (a, b, c) = tuple3
val a: Int = 1
val b: String = hello
val c: Double = 2.0

scala> a
val res2: Int = 1

scala> c
val res3: Double = 2.0
```

元组最大长度为 22，可以通过 `_1`，`_2` 来访问元素。我们可以一次为元组的每个元素声明变量。

### 实现simulatePlan

``` scala
  def simulatePlan(
      interestRate: Double,
      nbOfMonthsSaving: Int,
      nbOfMonthsInRetirement: Int,
      netIncome: Int,
      currentExpenses: Int,
      initialCapital: Double
  ): (Double, Double) = {
    val capitalAtRetirement = futureCapital(
      interestRate = interestRate,
      nbOfMonths = nbOfMonthsSaving,
      netIncome = netIncome,
      currentExpenses = currentExpenses,
      initialCapital = initialCapital
    )

    val capitalAfterDeath = futureCapital(
      interestRate = interestRate,
      nbOfMonths = nbOfMonthsInRetirement,
      netIncome = 0,
      currentExpenses = currentExpenses,
      initialCapital = capitalAtRetirement
    )

    (capitalAtRetirement, capitalAfterDeath)
  }
```

现在测试通过了。


# 计算何时退休

尝试了 `simulatePlan` 函数之后，就想输入不同的月份来观察退休和死亡的结果。这将有利于找出最优的退休时间 `nbOfMonths`，以便于在退休后的日子里有足够的资金。

## 测试失败的nbOfMonthsSaving

``` scala
    "nbOfMonthsSaving" should {
      "calculate how long I need to save before I can retire" in {
        val actual = RetCalc.nbOfMonthsSaving(
          interestRate = 0.04 / 12,
          nbOfMonthsInRetirement = 40 * 12,
          netIncome = 3000,
          currentExpenses = 2000,
          initialCapital = 10000
        )

        val excepted = 23 * 12 + 1
        actual should ===(excepted)
      }
    }
```

这个测试中，期望值有点难以计算。一种方法是使用 Excel 的 `NPM` 函数。另外，我们可以多次调用 `simulatePlan` 来计算，递增 `nbOfMonthsSaving` 来找出最优值。 

## 编写函数体
在函数编程中，我们避免变量的改变。在命令式语言中，可以通过使用 `while` 循环来实现 `nbOfMonthsSaving`，在 Scala 中也可以这样做，但是最佳时间是使用不可变的变量。一种很好的解决方案是使用递归：

``` scala
  def nbOfMonthsSaving(
      interestRate: Double,
      nbOfMonthsInRetirement: Int,
      netIncome: Int,
      currentExpenses: Int,
      initialCapital: Double
  ): Int = {
    def loop(months: Int): Int = {
      val (capitalAtRetirement, capitalAfterDeath) = simulatePlan(
        interestRate = interestRate,
        nbOfMonthsSaving = months,
        nbOfMonthsInRetirement = nbOfMonthsInRetirement,
        netIncome = netIncome,
        currentExpenses = currentExpenses,
        initialCapital = initialCapital
      )
      val returnValue =
        if (capitalAfterDeath > 0.0) months else loop(months + 1)
      returnValue
    }
    loop(0)
  }
```
我们在函数体内部声明递归函数，它不可以在其他地方使用。`loop` 函数的 `months` 每次递增 1 ，直到计算出 `capitalAfterDeath` 为正数。`loop` 函数最初的值为 `months = 0`。

现在，我们测试就可以通过了。

## 理解尾部递归
添加下面的测试：

``` scala
    "nbOfMonthsSaving" should {
      "calculate how long I need to save before I can retire" in {
        // ...
      }

      "not crash if the resulting nbOfMonths is very high" in {
        val actual = RetCalc.nbOfMonthsSaving(
          interestRate = 0.01 / 12,
          nbOfMonthsInRetirement = 40 * 12,
          netIncome = 3000,
          currentExpenses = 2999,
          initialCapital = 0
        )
        val expected = 8280
        actual should ===(expected)
      }
    }
```
运行测试，将会跑出 `StackOverflowError`，这是因为每次 `loop` 被调用递归，本地的变量就会存储在 JVM 堆栈，而我们知道堆栈是很小的，很容易的满了。幸运的是，Scala 编译器中有一种机制可以自动的转换成尾部递归调用 `while` 循环。我们称在 `loop` 中调用 `loop` 的递归为尾部递归（tail-recursive）。

我们可以很容易地将之前的代码转换成尾部递归。

``` scala
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
```

通常来说，超过 100 次的递归都应该使用尾部递归，尾部递归使用 `@tailrec` 注解可以让编译器校验它是否是尾部递归。

## 确保终止
假设你的开销比赚的要多，将存不够钱退休，是指要成千上万年！像这样：

``` scala
    "nbOfMonthsSaving" should {
      "calculate how long I need to save before I can retire" in {
        // ...
      }

      "not crash if the resulting nbOfMonths is very high" in {
        // ...
      }

      "not loop forever if I enter bad parameters" in {
        val actual = RetCalc.nbOfMonthsSaving(
          interestRate = 0.04 / 12,
          nbOfMonthsInRetirement = 40 * 12,
          netIncome = 1000,
          currentExpenses = 2000,
          initialCapital = 10000
        )
        actual should ===(Int.MaxValue)
      }
    }
```

# 使用市场利率
在我们的计算中，一直假设利率是常量，但是实际中复杂得多。用市场数据中的实际利率来对我们的退休计划获得更多信心，这将更为准确。为此，我们首先需要修改代码，以便使用可变的利率执行相同的计算。之后，我们将加载真实的市场数据，通过跟踪*标准普尔 500 指数*（S & P 500）来模拟基金的常规投资。

## 定义代数数据类型
为了支持可变利率，我们需要修改包含 `interestRate: Double` 参数的所有函数，相对于 `Double`，我们需要可以表示常量利率或一系列利率的类型。

考虑两种类型 `A` 和 `B`，我们之前知道如何定义类型 `A` 和 `B`。生产上，我们可以使用元组定义，例如 `ab: (A, B)`，或 `case class`，例如 `case class MyProduct(a: A, b: B)`。

换而言之，类型既可以是 `A` 也可以试 `B`，在 Scala 中我们使用 `sealed` 声明特质的继承：

``` scala
sealed trait Shape
case class Circle(diameter: Double) extends Shape
case class Rectangle(width: Double, height: Double) extends Shape
```
**代数数据类型**（Algebraic Data Type）是由求和类型和乘积类型组成的数据类型，用来定义数据结构。前面的代码，我们定义了代数类型 `Shape`，组成求和类型（sum type）—— `Shape` 可以是 `Circle` 或 `Rectangle`，以及乘积类型（product type）`Rectangle` —— 它有 width 和 height 。

关键字 `sealed` 表示特质的子类必须声明在同一个 `.scala` 文件中，如果尝试在另一个文件声明一个类来继承 `sealed` 特质，编译器会拒绝。这是用来保证我们的继承树是完整的，正如我们稍后将看到的，它在使用模式匹配时的好处是很有意思的。

回到我们的问题，我们定义一个代数数据类型 `Returns`：

`src/main/scala/retcalc/Returns.scala`:

``` scala
package retcalc

sealed trait Returns
case class FixedReturns(annualRate: Double) extends Returns
case class VariableReturns(returns: Vector[VariableReturn]) extends Returns
case class VariableReturn(monthId: String, monthlyRate: Double)
```

对于 `VariableReturn`，我们保持月利率和标识 `monthId` （*2017.02* 标识 2017 年 2 月）。推荐使用向量 `Vector` 来构建一系列元素。`Vector` 在添加、插入（appending/inserting）元素以及通过索引访问元素方面比 `List` 快。

## 筛选特定期间的回报
当我们在很长时间（比如 1900 年到 2017 年）有 `VariableReturn`，可以使用较小周期来模拟 50 年的返回值.

我们在 `VariableReturn` 类中创建方法，并返回包含特定时段的回报：

`ReturnsSpec.scala`:

``` scala
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
```

首先，我们生成一个 `returns` 的序列并通过 `Vector.tabulate` 赋值给 `variableReturns`，它生成 12 个元素，每个元素都通过匿名函数将参数 `i` 从 `0` 到 `1` 遍历。然后调用 `VariableReturn` 构造函数，生成字符串 `2017.01`，`2017.02` 等。

函数 `fromUtils` 返回指定区间的 `VariableReturns`：

``` scala
case class VariableReturns(returns: Vector[VariableReturn]) extends Returns {
  def fromUtils(monthIdFrom: String, monthIdUntil: String): VariableReturns =
    VariableReturns(
      returns
        .dropWhile(_.monthId != monthIdFrom)
        .takeWhile(_.monthId != monthIdUntil)
    )
}
```

## 模式匹配
现在，我们可以表达变量 `returns`，我们需要修改 `futureCapital` 来传入 `Returns` 参数替代每月的 `Double` 类型的利率：

``` scala
      "calculate the amount of savings I will have in n months" in {
        // Excel = FV(0.04/12, 25*12, 1000, 10000, 0)
        val actual =
          RetCalc.futureCapital(
            returns = FixedReturns(0.04),
            nbOfMonths = 25 * 12,
            netIncome = 3000,
            currentExpenses = 2000,
            initialCapital = 10000
          )
        val expected = 541267.1990
        actual should ===(expected)
      }
```
然后，修改 `futureCapital` 函数：

``` scala
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
        .monthlyRate(fixedReturns, month)) + monthlySavings
    )
  }
```

在 `Returns.scala` 中创建单例：

``` scala
object Returns {
  def monthlyRate(returns: Returns, month: Int): Double =
    returns match {
      case FixedReturns(r) => r / 12
    }
}
```

在 `ReturnsSpec` 中创建测试单元：

``` scala
  "Returns" when {
    "monthlyRate" should {
      "return a fixed rate for a FixedReturn" in {
        Returns.monthlyRate(FixedReturns(0.04), 0) should ===(0.04 / 12)
        Returns.monthlyRate(FixedReturns(0.04), 10) should ===(0.04 / 12)
      }

      val variableReturns = VariableReturns(
        Vector(VariableReturn("2000.01", 0.1), VariableReturn("2000.02", 0.2))
      )

      "return the nth rate for VariableReturn" in {
        Returns.monthlyRate(variableReturns, 0) should ===(0.1)
        Returns.monthlyRate(variableReturns, 1) should ===(0.2)
      }
      "roll over from the first rate if n > length" in {
        Returns.monthlyRate(variableReturns, 2) should ===(0.1)
        Returns.monthlyRate(variableReturns, 3) should ===(0.2)
        Returns.monthlyRate(variableReturns, 4) should ===(0.1)
      }
    }
  }
```

``` scala
object Returns {
  def monthlyRate(returns: Returns, month: Int): Double =
    returns match {
      case FixedReturns(r)     => r / 12
      case VariableReturns(rs) => rs(month % rs.length).monthlyRate
    }
}
```

这里的例子比较简单，但也可以实现复杂的模式。强大到通常可以替代 `if/else` 表达式：

``` scala
scala> Vector(1, 2, 3, 4) match {
     |   case head +: second +: tail => tail
     | }
val res5: scala.collection.immutable.Vector[Int] = Vector(3, 4)

scala> Vector(1, 2, 3, 4) match {
     |   case head +: second +: tail => second
     | }
val res6: Int = 2

scala> ("0", 1, (2.0, 3.0)) match {
     |   case ("0", int, (d0, d1)) => d0 + d1
     | }
val res7: Double = 5.0

scala> "hello" match {
     |   case "hello" | "world" => 1
     |   case "hello world world" => 2
     | }
val res8: Int = 1
```
我是函数式编程的的倡导者，更喜欢在对象中使用纯函数：

- 因为整个调度逻辑在一个地方，所以它们更容易推理。
- 很容易一直到其他对象，有助于重构。
- 它们的范围更为有限。在类方法中，始终在作用域中拥有类的所有属性。在函数中，只有函数的参数。这有助于单元测试和可读性，因为您知道函数除了参数之外不能使用其他任何东西。另外，当类具有可变属性时，它可以避免副作用。
- 有时在面向对象的设计中，当一个方法操作两个对象A 和 B 时，不清楚该方法应该在类 A 还是类 B 中。

## 重构simulatePlan

我们修改了 `futureCapital`，同样地也需要修改调用它的函数，目前只有 `simulatePlan`。之前的利息我们是通过固定利率直接实现的，而现在使用可变的利率。

例如，我们从 1950 年开始存钱并在 1975 年退休，在储蓄阶段，我们需要计算从 1950 到 1975 年的回报；退休后则需从 1975 年算起。我们创建新的测试单元以确保两个阶段使用不同的 `returns`。

`RetCalcSpec.scala`:

``` scala
    "simulatePlan" should {
      "calculate the capital at retirement and capital after death" in {
        val (capitalAtRetirement, capitalAfterDeath) = RetCalc.simulatePlan(
          returns = FixedReturns(0.04),
          params = params,
          nbOfMonthsSavings = 25 * 12
        )
        capitalAtRetirement should ===(541267.1990)
        capitalAfterDeath should ===(309867.5316)
      }

      "use different returns for capitalisation and  drawdown" in {
        val nbOfMonthsSavings = 25 * 12
        val returns = VariableReturns(
          Vector.tabulate(nbOfMonthsSavings + params.nbOfMonthsInRetirement)(
            i =>
              if (i < nbOfMonthsSavings)
                VariableReturn(i.toString, 0.04 / 12)
              else
                VariableReturn(i.toString, 0.03 / 12)
          )
        )
        val (capitalAtRetirement, capitalAfterDeath) =
          RetCalc.simulatePlan(returns, params, nbOfMonthsSavings)
        capitalAtRetirement should ===(541267.1990)
        capitalAfterDeath should ===(-57737.7227)
      }
    }

    "nbOfMonthsSaving" should {
      "calculate how long I need to save before I can retire" in {
        val actual = RetCalc.nbOfMonthsSaving(
          returns = FixedReturns(0.04),
          params = params
        )

        val excepted = 23 * 12 + 1
        actual should ===(excepted)
      }

      "not crash if the resulting nbOfMonths is very high" in {
        val actual = RetCalc.nbOfMonthsSaving(
          returns = FixedReturns(0.01),
          params = RetCalcParams(
            nbOfMonthsInRetirement = 40 * 12,
            netIncome = 3000,
            currentExpenses = 2999,
            initialCapital = 0
          )
        )
        val expected = 8280
        actual should ===(expected)
      }

      "not loop forever if I enter bad parameters" in {
        val actual = RetCalc.nbOfMonthsSaving(
          FixedReturns(0.04),
          params = params.copy(netIncome = 1000)
        )
        actual should ===(Int.MaxValue)
      }
    }
```

`RetCalc.scala`:

``` scala
  def simulatePlan(
      returns: Returns,
      params: RetCalcParams,
      nbOfMonthsSavings: Int,
      monthOffset: Int = 0
  ): (Double, Double) = {
    import params._
    val capitalAtRetirement = futureCapital(
      returns = OffsetReturns(returns, monthOffset),
      nbOfMonths = nbOfMonthsSavings,
      netIncome = netIncome,
      currentExpenses = currentExpenses,
      initialCapital = initialCapital
    )

    val capitalAfterDeath = futureCapital(
      returns = OffsetReturns(returns, monthOffset + nbOfMonthsSavings),
      nbOfMonths = nbOfMonthsInRetirement,
      netIncome = 0,
      currentExpenses = currentExpenses,
      initialCapital = capitalAtRetirement
    )

    (capitalAtRetirement, capitalAfterDeath)
  }

  def nbOfMonthsSaving(
      returns: Returns,
      params: RetCalcParams
  ): Int = {
    @tailrec
    def loop(months: Int): Int = {
      val (_, capitalAfterDeath) = simulatePlan(
        returns = returns,
        params = params,
        nbOfMonthsSavings = months
      )
      if (capitalAfterDeath > 0.0) months else loop(months + 1)
    }
    if (params.netIncome > params.currentExpenses) loop(0) else Int.MaxValue
  }
```

这里我们引用新的子类 `OffsetReturns` 来让 `futureCapital` 调用，它将转移开始月份。我们先编写测试单元：

`ReturnsSpec.scala`:

``` scala
      "return the n+offset th  rate for OffsetReturn" in {
        val returns = OffsetReturns(variableReturns, 1)
        Returns.monthlyRate(returns, 0) should ===(0.2)
        Returns.monthlyRate(returns, 1) should ===(0.1)
      }
```


接下来，我们完成 `Returns.scala`:

``` scala
case class OffsetReturns(orig: Returns, offset: Int) extends Returns

object Returns {
  def monthlyRate(returns: Returns, month: Int): Double =
    returns match {
      case FixedReturns(r)           => r / 12
      case VariableReturns(rs)       => rs(month % rs.length).monthlyRate
      case OffsetReturns(rs, offset) => monthlyRate(rs, month + offset)
    }
}
```

运行 `sbt test`，测试成功。

```
[info] ReturnsSpec:
[info] VariableReturns
[info]   when formUntil
[info]   - should keep only a window of the returns
[info] Returns
[info]   when monthlyRate
[info]   - should return a fixed rate for a FixedReturn
[info]   - should return the nth rate for VariableReturn
[info]   - should roll over from the first rate if n > length
[info]   - should return the n+offset th  rate for OffsetReturn
[info] RetCalcSpec:
[info] RetCalc
[info]   when futureCapital
[info]   - should calculate the amount of savings I will have in n months
[info]   - should calculate how much savings will be left after having taken a pension for n months
[info]   when simulatePlan
[info]   - should calculate the capital at retirement and capital after death
[info]   - should use different returns for capitalisation and  drawdown
[info]   when nbOfMonthsSaving
[info]   - should calculate how long I need to save before I can retire
[info]   - should not crash if the resulting nbOfMonths is very high
[info]   - should not loop forever if I enter bad parameters
[info] Run completed in 1 second, 907 milliseconds.
[info] Total number of tests run: 12
[info] Suites: completed 2, aborted 0
[info] Tests: succeeded 12, failed 0, canceled 0, ignored 0, pending 0
[info] All tests passed.
[success] Total time: 11 s
```

## 加载市场数据
为了计算更为真实，我们采用标准普尔500指数（S & P 500）。

### 构建测试单元

`EquityDataSpec.scala`：

``` scala
package retcalc

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EquityDataSpec extends AnyWordSpec with Matchers {
  "EquityData" when {
    "fromResource" should {
      "load mark data from a tsv file" in {
        val data = EquityData.fromResource("sp500_2017.tsv")
        data should ===(
          Vector(
            EquityData("2016.09", 2157.69, 45.03),
            EquityData("2016.10", 2143.02, 45.25),
            EquityData("2016.11", 2164.99, 45.48),
            EquityData("2016.12", 2246.63, 45.7),
            EquityData("2017.01", 2275.12, 45.93),
            EquityData("2017.02", 2329.91, 46.15),
            EquityData("2017.03", 2366.82, 46.38),
            EquityData("2017.04", 2359.31, 46.66),
            EquityData("2017.05", 2395.35, 46.94),
            EquityData("2017.06", 2433.99, 47.22),
            EquityData("2017.07", 2454.10, 47.54),
            EquityData("2017.08", 2456.22, 47.85),
            EquityData("2017.09", 2492.84, 48.17)
          )
        )
      }
    }
    "monthlyDividend" should {
      "return a monthly dividend" in {
        EquityData("2016.09", 2157.69, 45.03).monthlyDividend should ===(
          45.03 / 12
        )

      }
    }
  }
}
```
### 从文件加载数据
`EquityData.scala`:

``` scala
package retcalc

import scala.io.Source

case class EquityData(monthId: String, value: Double, annualDividend: Double) {
  val monthlyDividend: Double = annualDividend / 12
}

object EquityData {
  def fromResource(resource: String): Vector[EquityData] =
    Source
      .fromResource(resource)
      .getLines()
      .drop(1)
      .map { line =>
        val fields = line.split(("\t"))
        EquityData(
          monthId = fields(0),
          value = fields(1).toDouble,
          annualDividend = fields(2).toDouble
        )
      }
      .toVector
}
```

`scala.io.Source.fromResource` 默认从本地的 *resource* 文件夹加载并返回 `Source` 对象，文件夹可以是 `src/test/resources` 或 `src/main/resources`。

`getLines` 返回 `Iterator[String]`，是可变的数据结构，允许我们迭代序列元素。它提供了很多从普通到集合的很多函数。这里我们去掉包含表头的第一行，并将每一行使用匿名函数通过 `map` 座转换。

``` scala
scala> val iterator = (1 to 3).iterator
val iterator: Iterator[Int] = <iterator>

scala> iterator foreach println
1
2
3

scala> iterator foreach println

scala>
```

文件 `sp500_2017.tsv`:

```
month	SP500	dividend
2016.09	2157.69	45.03
2016.10	2143.02	45.25
2016.11	2164.99	45.48
2016.12	2246.63	45.7
2017.01	2275.12	45.93
2017.02	2329.91	46.15
2017.03	2366.82	46.38
2017.04	2359.31	46.66
2017.05	2395.35	46.94
2017.06	2433.99	47.22
2017.07	2454.10	47.54
2017.08	2456.22	47.85
2017.09	2492.84	48.17
```
### 加载通货膨胀数据
`InflationDataSpec.scala`:

``` scala
package retcalc;

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InflationDataSpec extends AnyWordSpec with Matchers {
  "InflationData" when {
    "fromResource" should {
      "load CPI data from a tsv file" in {
        val data = InflationData.fromResource("cpi_2017.tsv")
        data should ===(
          Vector(
            InflationData("2016.09", 241.428),
            InflationData("2016.10", 241.729),
            InflationData("2016.11", 241.353),
            InflationData("2016.12", 241.432),
            InflationData("2017.01", 242.839),
            InflationData("2017.02", 243.603),
            InflationData("2017.03", 243.801),
            InflationData("2017.04", 244.524),
            InflationData("2017.05", 244.733),
            InflationData("2017.06", 244.955),
            InflationData("2017.07", 244.786),
            InflationData("2017.08", 245.519),
            InflationData("2017.09", 246.819)
          )
        )
      }
    }
  }
}
```

`InflationData.scala`:

``` scala
package retcalc

import scala.io.Source

case class InflationData(monthId: String, value: Double)

object InflationData {
  def fromResource(resource: String): Vector[InflationData] =
    Source
      .fromResource(resource)
      .getLines()
      .drop(1)
      .map { line =>
        val fields = line.split("\t")
        InflationData(monthId = fields(0), value = fields(1).toDouble)
      }
      .toVector
}
```
## 计算真实回报
给出月份 *n* ，实际回报 ![](https://latex.codecogs.com/gif.latex?\dpi{100}return_n%20-%20inflation_n)，公式如下：

<div align="center">
![](https://latex.codecogs.com/gif.latex?\dpi{100}realReturn_n%20=%20\frac{price_n%20+%20dividends_n}{price_{n-1}}%20-%20\frac{inflation_n}{inflation_{n-1}})
</div>

我们在 `Returns` 里通过 `Vector[EquityData]` 和 `Vector[InflationData]` 创建新的 `VariableReturns`。首先添加测试单元到 `ReturnsSpec`：

``` scala
  "Returns" when {
    "monthlyRate" should {
      // ...
    }

    "fromEquityAndInflationData" should {
      "compute real total returns from equity and inflation data" in {
        val equities = Vector(
          EquityData("2117.01", 100.0, 10.0),
          EquityData("2117.02", 101.0, 12.0),
          EquityData("2117.03", 102.0, 12.0))
        val inflations = Vector(
          InflationData("2117.01", 100.0),
          InflationData("2117.02", 102.0),
          InflationData("2117.03", 102.0))

        val returns = Returns.fromEquityAndInflationData(equities, inflations)
        returns should ===(
          VariableReturns(
            Vector(
              VariableReturn(
                "2017.02",
                (101.0 + 12.0 / 12) / 100.0 - 102.0 / 100.0),
              VariableReturn(
                "2117.03",
                (102.0 + 12.0 / 12) / 101.0 - 102.0 / 102.0))))
      }
    }
  }
```

我们创建了 `EquityData` 和 `InflationData` 两个 `Vector` 实例，并通过前面的公式做计算。

接着在 `Returns.scala` 中实现 `fromEquityAndInflationData` 函数：

``` scala
object Returns {
  def fromEquityAndInflationData(
      equities: Vector[EquityData],
      inflations: Vector[InflationData]
  ): VariableReturns = {
    VariableReturns(
      returns = equities
        .zip(inflations)
        .sliding(2)
        .collect {
          case (prevEquity, prevInflation) +: (equity, inflation) +: Vector() =>
            val inflationRate = inflation.value / prevInflation.value
            val totalReturn =
              (equity.value + equity.monthlyDividend) / prevEquity.value
            val realTotalReturn = totalReturn - inflationRate

            VariableReturn(equity.monthId, realTotalReturn)
        }
        .toVector)
  }

  def monthlyRate(returns: Returns, month: Int): Double =
    // ...
    
}
```

首先，我们通过 `zip` 将两个 `Vector` 创建成元组 `(EquityData, InflationData)`。比如：

``` scala
scala> Vector(1,2).zip(Vector("a", "b", "c"))
res0: scala.collection.immutable.Vector[(Int, String)] = Vector((1,a), (2,b))
```
这是个好的开端，接下来迭代集合可以分别得到 *price*, *diviends* 和 *inflation* 的 *n* 次方，但为了计算我们的公式，我们也需要前一个（*n-1*）数据，为此我们使用 `sliding(2)`。尝试一下：

``` scala
scala>  val it = Vector(1, 2, 3, 4).sliding(2)
val it: Iterator[scala.collection.immutable.Vector[Int]] = <iterator>

scala> it.toVector
val res30: scala.collection.immutable.Vector[scala.collection.immutable.Vector[Int]] = Vector(Vector(1, 2), Vector(2, 3), Vector(3, 4))

scala> Vector(1).sliding(2).toVector
val res31: scala.collection.immutable.Vector[scala.collection.immutable.Vector[Int]] = Vector(Vector(1))
```
`sliding(p)` 可以创建大小为 `p` 的 `Iterator` 集合。

``` scala
scala> val v = Vector(1, 2, 3)
val v: scala.collection.immutable.Vector[Int] = Vector(1, 2, 3)

scala> v.filter(i => i != 2).map(_ + 1)
val res33: scala.collection.immutable.Vector[Int] = Vector(2, 4)

scala> v.collect { case i if i != 2 => i + 1 }
val res34: scala.collection.immutable.Vector[Int] = Vector(2, 4)
```

最后，我们就行匹配：

``` scala
case (prevEquity, prevInflation) +: (equity, inflation) +: Vector() =>
```

# 打包应用 
## 创建应用项目
我们来创建可执行的 `RetCalc.simulatePlan`，通过一系列空格分隔的参数调用，并将结果打印在终端。

这种测试是集成集中组件并使用市场数据集，也就是说，我们不再使用任何测试单元，而是一种集成测试。基于此，我们使用后缀 `IT` 来代替 `Spec`。

首先，从网上复制 [sp500.tsv](https://github.com/PacktPublishing/Scala-Programming-Projects/blob/master/Chapter02/retirement-calculator/src/main/resources/sp500.tsv) 和 [cpi.tsv](https://github.com/PacktPublishing/Scala-Programming-Projects/blob/master/Chapter02/retirement-calculator/src/main/resources/cpi.tsv) 数据到 `src/main/resources`，然后在 `src/test/scala` 创建测试单元 `SimulatePlanAppIt`：

``` scala
package retcalc

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SimulatePlanAppIT
    extends AnyWordSpec
    with Matchers
    with TypeCheckedTripleEquals {
  "SimulatePlanApp" when {
    "strMain" should {
      "simulate a retirement plan using market returns" in {
        val actualResult = SimulatePlanApp.strMain(
          Array("1997.09,2017.09", "25", "40", "3000", "2000", "10000"))
        val expectedResult =
          s"""
             |Capital after 25 years of savings:    499923
             |Capital after 40 years in retirement: 586435
             |""".stripMargin
        actualResult should ===(expectedResult)
      }
    }
  }
}
```


为了保持简单，我们假设参数有特定的顺序。我们将在下一章开发更多友好而有用的接口。本章将完成：

1. 我们使用 returns 变量，通过逗号（comma）分离。
1. 省钱的年数。
1. 退休后的年数。
1. 收入。
1. 开销。
1. 初始资金。


`SimulatePlanApp.scala`:
``` scala
package retcalc

object SimulatePlanApp extends App {
  println(strMain(args))
  def strMain(args: Array[String]): String = {
    val (from +: until +: Nil) = args(0).split(",").toList
    val nbOfYearsSaving = args(1).toInt
    val nbOfYearsInRetirement = args(2).toInt

    val allReturns =
      Returns.fromEquityAndInflationData(
        equities = EquityData.fromResource("sp500.tsv"),
        inflations = InflationData.fromResource("cpi.tsv"))
    val (capitalAtRetirement, capitalAfterDeath) = RetCalc.simulatePlan(
      returns = allReturns.fromUtils(from, until),
      params = RetCalcParams(
        nbOfMonthsInRetirement = nbOfYearsInRetirement * 12,
        netIncome = args(3).toInt,
        currentExpenses = args(4).toInt,
        initialCapital = args(5).toInt),
      nbOfMonthsSavings = nbOfYearsSaving * 12
    )
    s"""
       |Capital after $nbOfYearsSaving years of savings:    ${capitalAtRetirement.round}
       |Capital after $nbOfYearsInRetirement years in retirement: ${capitalAfterDeath.round}
       |""".stripMargin

  }
}
```
## 打包应用
到目前为止一切都很顺利，当时我们想将应用发送给 Bob 的时候，他也能同样计算退休计划吗？对他来说去下载 IntelliJ 或 SBT 是很不方便的。 我们将应用打包成 `.jar` 文件以便于通过命令行运行。

SBT 提供包任务来创建 `.jar` 文件，但是这个文件不包含其他依赖。为了连同依赖一起打包，我们使用 [sbt-assembly](https://github.com/sbt/sbt-assembly) 插件。


# 总结
