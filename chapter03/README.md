# Handling Errors

这一章，我们继续之前的退休金计算项目。只要向计算器传递正确的参数，计算器就可以正常工作，但如果任何参数错误，则会出现严重的堆栈跟踪失败。我们的持续只适用于我们所说的 “幸福之路”。

编写生产软件的实际情况是，各种错误场景都可能发生。有些是可恢复的，有些必须以吸引人的方式呈现给用户，而且，对于一些与硬件相关的错误，我们可能需要让程序崩溃。

本章介绍异常处理，解释什么是引用透明（referential transparency），并试着让你相信异常不是处理错误的最佳方法。然后，我们将解释如何使用函数式编程构造来有效地处理错误的可能性。

在每一节中，我们将简要介绍一个新概念，然后在 Scala 工作表中使用它来了解如何使用它。之后，我们将应用这些新知识来改进退休计算器。

本章涵盖：
- 必要时使用异常
- 了解引用透明性
- 使用 `Option` 表示可选值
- 使用 `Either` 处理错误顺序
- 使用 `Validated` 以并行方式处理错误



# 使用异常
## 抛出异常
异常是 Scala 中用来处理错误场景的机制之一。它由两个语句组成：
- `throw exceptionObject` 语句终止当前的函数并调用异常
- `try { myFunc() } catch { case pattern1 => recoverExpr1 }` 语句捕获函数 `myFunc()` 的异常并匹配 `catch` 代码块
  - 如果是 `myFunc` 抛出的，但没有匹配到异常，那么函数终止并且异常被调用。如果没有 `try ... catch` 代码块将捕获异常，整个程序终止。
  - 如果匹配到 `pattern1`，将返回 `recoverExpr1`。
  - 如果没有异常，将返回 `myFunc()`。
  
``` scala
scala> :paste
// Entering paste mode (ctrl-D to finish)

case class Person(name: String, age: Int)

case class AgeNegativeException(message: String) extends Exception(message)

def createPerson(description: String): Person = {
  val split = description.split(" ")
  val age = split(1).toInt
  if (age < 0)
    throw AgeNegativeException(s"age: $age should be > 0")
  else
    Person(split(0), age)
}


// Exiting paste mode, now interpreting.

class Person
class AgeNegativeException
def createPerson(description: String): Person

scala> createPerson("John 25")
val res36: Person = Person(John,25)

scala> createPerson("John25")
java.lang.ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1
  at createPerson(<pastie>:7)
  ... 32 elided

scala> createPerson("John -25")
AgeNegativeException: age: -25 should be > 0
  at createPerson(<pastie>:9)
  ... 32 elided

scala> createPerson("John 25.3")
java.lang.NumberFormatException: For input string: "25.3"
  at java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:68)
  at java.base/java.lang.Integer.parseInt(Integer.java:652)
  at java.base/java.lang.Integer.parseInt(Integer.java:770)
  at scala.collection.StringOps$.toInt$extension(StringOps.scala:889)
  at createPerson(<pastie>:7)
  ... 32 elided
```

## 捕获异常

``` scala
scala> :paste
// Entering paste mode (ctrl-D to finish)

def averageAge(descriptions: Vector[String]): Double = {
  val total = descriptions.map(createPerson).map(_.age).sum
  total / descriptions.length
}


// Exiting paste mode, now interpreting.

def averageAge(descriptions: Vector[String]): Double
```

``` scala
scala> :paste
// Entering paste mode (ctrl-D to finish)

import scala.util.control.NonFatal

def personsSummary(personsInput: String): String = {
  val descriptions = personsInput.split("\n").toVector
  val avg = try {
    averageAge(descriptions)
  } catch {
    case e: AgeNegativeException =>
      println(s"one of the persons has a negative age: $e")
      0
    case NonFatal(e) =>
      println(s"something was wrong in the input: $e")
      0
  }
  s"${descriptions.length} persons with an average age of $avg"
}


// Exiting paste mode, now interpreting.

import scala.util.control.NonFatal
def personsSummary(personsInput: String): String
```
``` scala
scala> personsSummary(
     | """John 25
     | Sharleen 45""".stripMargin)
val res44: String = 2 persons with an average age of 35.0
scala> personsSummary(
     | """John 25
     | Sharleen45""".stripMargin)
something was wrong in the input: java.lang.ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1
val res46: String = 2 persons with an average age of 0.0

scala> personsSummary(
     | """John -25
     | Sharleen 45""".stripMargin)
one of the persons has a negative age: $line51.$read$$iw$AgeNegativeException: age: -25 should be > 0
val res48: String = 2 persons with an average age of 0.0
```

## 使用finally块

``` scala
import java.io.IOException
import java.net.URL
import scala.annotation.tailrec

val stream = new URL("https://www.packtpub.com/").openStream()
val htmlPage: String =
  try {
    @tailrec
    def loop(builder: StringBuilder): String = {
      val i = stream.read()
      if (i != -1)
        loop(builder.append(i.toChar))
      else
        builder.toString()
    }
    loop(StringBuilder.newBuilder)
  } catch {
    case e: IOException => s"cannot read URL: $e"
  }
  finally {
    stream.close()
  }
```
在实际项目中，通常使用：

``` scala
val htmlPage2 = scala.io.Source.fromURL("https://www.packtpub.com/").mkString
```

# 确保引用透明
在任何上下文中，当表达式可以被其值替换而不改变程序的行为时，我们称这种异常是**引用透明**（referentially transparent）的。

当一个表达式是一个函数调用时，这意味着我们总是可以用函数的返回值来代替这个函数调用。在任何上下文中保证这一点的函数称为**纯函数**（pure function）。

纯函数类似数学函数 —— 返回值只依赖传给函数的参数，调用时不需要考虑任何其他上下文的影响。

## 定义纯函数
下面的 `pureSquare` 是纯函数：

``` scala
scala> def pureSquare(x: Int): Int = x * x
def pureSquare(x: Int): Int

scala> val pureExpr = pureSquare(4) + pureSquare(3)
val pureExpr: Int = 25

scala> val pureExpr2 = 16 + 9
val pureExpr2: Int = 25
```
函数 `pureSquare(4)` 和 `pureSquare(3)` 是引用透明的 —— 当更新时函数有新的返回值，但语言的行为不变。

换言之，下面的是非纯函数：

``` scala
scala> var globalState = 1
var globalState: Int = 1

scala> def impure(x: Int): Int = {
     |   globalState = globalState + x
     |   globalState
     | }
def impure(x: Int): Int

scala> val impureExpr = impure(3)
val impureExpr: Int = 4

scala> val impureExpr = impure(4)
val impureExpr: Int = 8
```

非纯函数是有副作用的，副作用可以存在以下场景：
- 改变全局变量
- 打印到终端
- 打开网络连接
- 文件读写数据
- 数据库读写数据
- 与外部的整合

下面是非纯函数的另一个例子：

``` scala
scala> import scala.util.Random
import scala.util.Random

scala> def impureRand(): Int = Random.nextInt()
def impureRand(): Int

scala> impureRand()
val res5: Int = -884035480

scala> val impureExprRand = impureRand() + impureRand()
val impureExprRand: Int = 741754628

scala> val impureExprRand2 = -884035480 + -884035480
val impureExprRand2: Int = -1768070960
```
每次调用 `impureRand()` 结果都会改变，所以说调用 `impureRand()` 不是引用透明，`impureRand` 也不是纯函数。事实上，`random()` 每次都会生成全局的随机数，我们也称这样的函数是**不确定的**（nondeterministic）—— 不能预先知道返回值。

我们可以重写非纯函数，让它变成纯函数：

``` scala
scala> def pureRand(seed: Int): Int = new Random(seed).nextInt()
def pureRand(seed: Int): Int

scala> pureRand(10)
val res6: Int = -1157793070

scala> val pureExprRand = pureRand(10) + pureRand(10)
val pureExprRand: Int = 1979381156

scala> val pureExprRand2 = -1157793070 + -1157793070
val pureExprRand2: Int = 1979381156
```
通过调用 `pureRand(seed)` ，函数会返回同样的值，称该函数是引用透明的，并且是纯函数。

再看一个例子：

``` scala
scala> def impurePrint(): Unit = println("Hello impure")
def impurePrint(): Unit

scala> val impureExpr1: Unit = impurePrint()
Hello impure
val impureExpr1: Unit = ()

scala> val impureExpr2: Unit = ()
val impureExpr2: Unit = ()
```

这个例子中，`impurePrint` 的返回值是 `Unit` 类型，这是 Scala 中独有的类型：值是 `()`。如果用 `()` 来替换对 `impurePrint()` 的调用，程序的行为被改变了 —— 之前会有内容打印到终端，但是 `()` 则没有内容打印。


## 最佳实践
引用透明是函数式编程中的关键概念，如果程序中大部分使用了纯函数，下面的事情将变得很容易：
- **理解程序在做什么** 能知道函数返回值只依赖的参数，无需思考它的状态或者变量的上下文，只需要关注参数。
- **测试函数** 再重申一次 —— 函数的返回值只依赖入参，测试就变得很简单。可以尝试不同的入参并确认返回值是否是预期的。
- **多线程编程** 当纯函数的行为不依赖全局状态，就可以在多线程或不同的机器并行执行它，返回值也不会改变。由于我们的CPU是用越来越多的核心构建的，这将帮助您编写更快的程序。

然而，在程序中不可能只有纯函数，因为从本质上讲，程序必须与外部世界交互。它必须打印一些内容，读取一些用户输入，或者在数据库中保存一些状态。在函数式编程中，最佳实践是在大多数代码基中使用纯函数，并将不纯正的副作用函数推到程序的边界。

例如，在第二章的退休金计算，我们大部分使用纯函数实现了退休计算器，一个有副作用的函数是在 `SimulatePlanApp` 中的 `println`，这是程序的边界。在 `EquityData.fromFile` 和 `InflationData.fromFile` 中也有副作用，这些函数用于读取文件。然而，资源文件在程序执行期间永不改变。对于给定的文件名，我们将始终获得相同的文件内容，并且我们可以在所有调用中替换 `fromFile` 的返回值，而无需更改程序的行为。在这种情况下，读取文件的副作用是不可见的，我们可以考虑这些文件读写的作用是纯洁。另一个副作用函数是 `strMain`，因为它可以抛出异常。在本章的其余部分中，我们将了解为什么抛出异常会破坏引用透明性，并将学习如何用更好的函数式编程结构替换它。


> 非纯函数满足以下两个标准：
>
> - 它返回 `Unit`。
>
> - 它不接受任何参数，但返回一个类型。由于它返回的内容不能通过使用其参数获得，所以它必须使用全局状态。

请注意，纯函数可以在函数体内部使用可变变量或副作用。只要调用方无法观察到这些影响，我们就认为函数是纯函数。在 Scala SDK 中使用可变变量实现的，以提高性能。例如，看看下面的 `TraversableOnce.foldLeft`:

``` scala
def foldLeft[B](z: B)(op: (B, A) => B): B = {
     var result = z
     this foreach (x => result = op(result, x))
     result
}
```
## 展示异常如何破坏引用透明
当函数抛出异常，它破坏了引用透明。这种情况下，我将告诉你为什么。首先，创建一个新的 Case Scala：

``` scala
scala> :paste
// Entering paste mode (ctrl-D to finish)

def area(width: Double, height: Double): Double = {
  if (width > 5 || height > 5)
    throw new IllegalArgumentException("too big")
  else
    width * height
}


// Exiting paste mode, now interpreting.

def area(width: Double, height: Double): Double
```
我们用以下参数调用 `area`：

``` scala
scala> :paste
// Entering paste mode (ctrl-D to finish)

val area1 = area(3, 2)
   val area2 = area(4, 2)
   val total = try {
     area1 + area2
   } catch {
     case e: IllegalArgumentException => 0
}


// Exiting paste mode, now interpreting.

val area1: Double = 6.0
val area2: Double = 8.0
val total: Double = 14.0
```

我们获取到 `total: Double = 14.0`，在前面的代码中，`area1` 和 `area2` 是引用透明的。和下面的代码是等效的：

``` scala
scala> :paste
// Entering paste mode (ctrl-D to finish)

 val total = try {
     area(3, 2) + area(4, 2)
   } catch {
     case e: IllegalArgumentException => 0
}


// Exiting paste mode, now interpreting.

val total: Double = 14.0
```
这里 `total` 和之前是相同的。程序的行为没有被改变，`area1` 和 `area2` 也是引用透明的。

但是，我们接着往下看：

``` scala
scala> :paste
// Entering paste mode (ctrl-D to finish)

val area1 = area(6, 2)
   val area2 = area(4, 2)

    val total = try {
     area1 + area2
   } catch {
     case e: IllegalArgumentException => 0
}


// Exiting paste mode, now interpreting.

java.lang.IllegalArgumentException: too big
  at area(<pastie>:3)
  ... 40 elided
```
这时候，我们看到了异常 `java.lang.IllegalArgumentException: too big`，因为 `area1` 的第一个人入参大于 5 。我们来看看内嵌的 `area1` 和 `area2` 发生了什么：

``` scala
scala> :paste
// Entering paste mode (ctrl-D to finish)

 val total = try {
     area(6, 2) + area(4, 2)
   } catch {
     case e: IllegalArgumentException => 0
}


// Exiting paste mode, now interpreting.

val total: Double = 0.0
```
我们看到 `total: Double = 0.0`，程序的行为被改变了，所以 `area1` 不是引用透明的。

我们证明了异常处理破坏了引用透明，因此抛出异常的函数是非纯函数。这使得程序更难理解，因为你必须考虑到变量的定义位置，才能理解程序的行为。行为将根据变量是在 `try` 块内部还是外部定义而改变。在这个简单的例子中，这似乎不是什么大问题，但是当一行中有多个 `try` 块的链接函数调用，匹配不同类型的异常时，它可能会变得令人望而生畏。

另一个缺点是函数的签名并不表示它可以引发异常。当您调用一个可以抛出异常的函数时，您必须查看它的实现，以确定它可以抛出什么类型的异常，以及在什么情况下。如果函数调用其他函数，则会使问题复杂化。您可以通过添加注释或 `@throws` 注释来适应这一点，以指示可以引发哪些异常类型，但当代码重构时，这些类型可能会过时。当我们调用一个函数时，我们只需要考虑它的签名。签名有点像一个合同，给出这些参数，我会返回一个结果。如果您必须查看实现以了解抛出了哪些异常，则意味着契约尚未完成：某些信息被隐藏。

我们现在知道如何抛出和捕获异常，以及为什么我们要谨慎使用它们。最好的做法是：
- 尽早捕获可恢复异常，并用特定的返回类型指示失败的可能性。
- 不捕获无法恢复的异常，例如磁盘已满、内存不足或其他灾难性故障。每当发生此类异常时，这将使您的程序崩溃，然后您应该在程序之外进行手动或自动恢复过程。

在本章的剩余部分，将展示如何使用 `Option`，`Either` 和 `Validated` 类来模拟失败的可能性。

# 使用Option

# 使用Either

# 使用ValidationNel

# 总结
