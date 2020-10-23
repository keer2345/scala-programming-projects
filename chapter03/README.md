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
Scala 的 `Option` 是表示可选值的代数数据类型（ADT）。它也被视为可以包含零个或一个元素的 `List`，如果使用过 Java, C++ 或 C#，则可以安全的替换掉 `null` 引用。
## 操作Option实例
下面是个简单定义代数数据类型 `Option` 的例子：

``` scala
sealed trait Option[+A]
case class Some[A](value: A) extends Option[A]
case object None extends Option[Nothing]
```
Scala SDK 提供了更多优雅的实现，这里的定义只是为了说明 `Option` 可以是以下两种类型之一：
- `Some(value)` 表示有值存在
- `None` 表示没有值存在

> `Option[+A]` 中 `A` 前面的 `+` 号意思是 `Option` 在类型 `A` 协变（covariant）的。我们将在第四章了解逆变（contravariance）。目前，我们仅仅知道如果 `B` 是 `A` 的子类型，那么 `Option[B]` 是 `Option[A]` 的子类型。而且，你可能注意到 `None` 实际上继承自 `Option[Nothing]` 而不是 `Option[A]`，这是因为 `case object` 不能接受类型参数。
>
> 在 Scala 中，`Nothing` 是最底层的类型，意味着它是任何类型的子类型。也就是说对于任意 `A`， `None` 都是 `Option[A]` 的子类型。

下面是使用不同类型的 `Option` 例子：

``` scala
scala> val opt0: Option[Int] = None
val opt0: Option[Int] = None

scala> val opt1: Option[Int] = Some(1)
val opt1: Option[Int] = Some(1)

scala> val list0 = List.empty[String]
val list0: List[String] = List()

scala> list0.headOption
val res0: Option[String] = None

scala> list0.lastOption
val res1: Option[String] = None

scala> val list3 = List("Hello", "World")
val list3: List[String] = List(Hello, World)

scala> list3.headOption
val res2: Option[String] = Some(Hello)

scala> list3.lastOption
val res3: Option[String] = Some(World)
```

- 前面两个例子展示了我们如何定义包含 `Int` 的 `Option` 类型。
- 接下来的例子在 `List` 中使用 `headOption` 和 `lastOption` 方法来展示 SDK 中有许多返回 `Option` 的安全函数。如果 `List` 是空的，函数通常返回 `None`，注意 SDK 也提供不安全的 `head` 和 `last` 方法，如果调用的是空 `List`，不安全的方法（unsafe methods）抛出异常，要是没有捕获异常的话会破坏我们的程序。

> SDK 的很多函数提供了返回 `Option` 的安全函数，以及等效的非安全函数（它们会抛出异常）。最好是总是使用安全的做法。

由于 `Option` 是代数数据类型，我们可以使用模式匹配来测试 `Option` 是否为 `None` 或 `Some`：

``` scala
scala> def personDescription(name: String, db: Map[String, Int]): String = {
     |   db.get(name) match {
     |     case Some(age) => s"$name is $age years old"
     |     case None => s"$name is not presen in db"
     |   }
     | }
def personDescription(name: String, db: Map[String,Int]): String

scala> val db = Map("John" -> 25, "Rob" -> 40)
val db: scala.collection.immutable.Map[String,Int] = Map(John -> 25, Rob -> 40)

scala> personDescription("John", db)
val res5: String = John is 25 years old

scala> personDescription("Michael", db)
val res6: String = Michael is not presen in db
```
`Map` 中的 `get(key)` 方法返回包含关键字相关值的 `Option`，如果该关键字在 `Map` 中不存在就返回 `None`。当使用 `Option` 时，模式匹配很自然地依赖 `Option` 的值触发不同的行为。

另一种方式是使用 `map` 和 `getOrElse`：

``` scala
scala> def personDesc(name: String, db: Map[String, Int]): String = {
     |   val optString: Option[String] = db.get(name).map(age => s"$name is $age years old")
     |   optString.getOrElse(s"$name is not present in db")
val db2: scala.collection.immutable.Map[String,Int] = Map(John -> 25, Rob -> 40)

scala> personDesc("John", db)
val res7: String = John is 25 years old

scala> personDescription("Michael", db)
val res8: String = Michael is not presen in db
```
我们看看之前是如何使用 `map` 转换向量元素的，这里确实使用了相同的 `Option` —— 如果 `Option` 非空的话通过匿名函数调用 option 的值，我们获得 `Option[String]`；如果 `Option` 为 `None` 的话调用 `getOrElse`，它是安全提取 `Option` 内容的好办法。

> 一般不在 `Option` 中使用 `.get` 方法 —— 而是使用 `.getOrElse`。`.get` 方法会在 `Option` 为 `None` 时抛出异常，这是不安全的。

## 通过yield合成转换

下面的代码使用了相同的 `db: Map[String, Int]`，包含不同人的年龄，这个简单的函数返回两个人的平均年龄：

``` scala
scala> def averageAgeA(name1: String, name2: String, db: Map[String, Int]): Option[Double] = {
     |   val optOptAvg: Option[Option[Double]] =
     |     db.get(name1).map(age1 =>
     |       db.get(name2).map(age2 =>
     |         (age1 + age2).toDouble / 2))
     |   optOptAvg.flatten
     | }
def averageAgeA(name1: String, name2: String, db: Map[String,Int]): Option[Double]

scala> val db = Map("John" -> 25, "Rob" -> 40)
val db: scala.collection.immutable.Map[String,Int] = Map(John -> 25, Rob -> 40)

scala> averageAgeA("John", "Rob", db)
val res16: Option[Double] = Some(32.5)

scala> averageAgeA("John", "Michael", db)
val res18: Option[Double] = None
```
函数返回类型是 `Option[Douible]`，如果 `name1` 或 `name2` 在 `db` Map 中未找到，则 `averageAgeA` 返回 `None`，如果名字都找到了，返回 `Some(value)`。使用 `map` 来转换 `Option[Double]` 包含的值。幸运的是，我们可以使用 `flatten` 来移除一层嵌套。 

我实现了 `averageAgeA`，但可以用 `flatMap` 来优化它：

``` scala
scala> def averageAgeB(name1: String, name2: String, db: Map[String, Int]): Option[Double] =
     |   db.get(name1).flatMap(age1 =>
     |     db.get(name2).map(age2 =>
     |       (age1 + age2).toDouble /2))
def averageAgeB(name1: String, name2: String, db: Map[String,Int]): Option[Double]

scala> averageAgeB("John", "Rob", db)
val res19: Option[Double] = Some(32.5)

scala> averageAgeB("John", "Michael", db)
val res20: Option[Double] = None
```

`flatMap` 等效于 `flatten` 和 `map`。在我们的函数中，用 `flatMap(...)` 替换了 `map(...).flatten`。

到目前为止，一切都很好，但是如果是三个人或四个人，我们如何获取到年龄的平均值？我们将内嵌多个 `flatMap`，这将大大地降低可读性。所幸，Scala 提供了语法糖可以让我们进一步简化函数，成为 `for`：

``` scala
scala> def averageAgeC(name1: String, name2: String, db: Map[String, Int]): Option[Double] =
     |   for {
     |     age1 <- db.get(name1)
     |     age2 <- db.get(name2)
     |   } yield (age1 + age2).toDouble /2
def averageAgeC(name1: String, name2: String, db: Map[String,Int]): Option[Double]
```
当编译了这段 `for`，类似 `for {...} yield {...}`，Scala 编译器转换成 `flatMap/map` 操作。它是这样工作的：
- 内部的 `for` 代码块，科颜氏一个或者多个类似 `variable <- context` 的表达式判断，称之为**生成器**（generator）。箭头的左边是变量名，它绑定右边了内容的上下文。
- 除了最后一个，每个生成器转成成 `flatMap` 表达式。
- 最后一个生成器转换成 `map` 表达式。
- 所有上下文表达式（箭头右边）必须有同样地上下文类型。

在前面的例子中，我们使用 `Option` 当做上下文的类型，但对于 `yield`，也可以与具有 `flatMap` 和 `map` 操作的任何类一起使用。例如，我们可以用带有 `Vector` 的 `for ... yield` 来运行内嵌循环：

``` scala
scala> for {
     |   i <- Vector("one", "two")
     |   j <- Vector(1,2,3)
     | } yield (i, j)
val res21: scala.collection.immutable.Vector[(String, Int)] = Vector((one,1), (one,2), (one,3), (two,1), (two,2), (two,3))
```

> **语法糖**是让编程语言变得容易读写的语法，让编程人员更愉悦。

## 使用Option重构退休金计算器

现在我们知道了 `Option` 能做什么，我们将要重构第二章退休金计算器的其中一个函数，优化边界场景。

在文件 `RetCala.scala` 中，我们改变 `nbMonthsSaving` 返回值的类型：

``` scala
def nbOfMonthsSaving(returns: Returns, params: RetCalcParams): Option[Int] = {
  @tailrec
  def loop(months: Int): Int = {
    val (_, capitalAfterDeath) = simulatePlan(
      returns = returns,
      params = params,
      nbOfMonthsSavings = months)
    if (capitalAfterDeath > 0.0) months else loop(months + 1)
  }
  if (params.netIncome > params.currentExpenses) Some(loop(0)) else None
}
```
测试会提示如下错误：

``` scala
types Option[Int] and Int do not adhere to the type constraint selected for the === and !== operators; the missing implicit parameter is of type org.scalactic.CanEqual[Option[Int],Int]
[error]         actual should ===(excepted)
```
这个错误意味着类型不匹配。`actual` 是 `Option[Int]` 类型，而 `expected` 是 `Int` 类型。我们需要做些修改：

``` scala
    "nbOfMonthsSaving" should {
      "calculate how long I need to save before I can retire" in {
        val actual = RetCalc.nbOfMonthsSaving(
          returns = FixedReturns(0.04),
          params = params
        )

        val excepted = 23 * 12 + 1
        actual should ===(Some(excepted))
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
        actual should ===(Some(expected))
      }

      "not loop forever if I enter bad parameters" in {
        val actual = RetCalc.nbOfMonthsSaving(
          FixedReturns(0.04),
          params = params.copy(netIncome = 1000)
        )
        actual should ===(None)
      }
    }
```

我们修改了 `actual should ===(Some(expected))` 和 `actual should ===(None)`，测试通过！

到目前为止我们可以建模安全的 Option 值了。然而，有时候 `None` 并不能表达确切的意思，为什么函数返回 `None`？是因为传递的参数是错误的吗？哪个参数错了？正确的应该是什么？为了更好的解释 `None` 是 为了理解为什么没有返回值。在下一节，我们将为此使用 `Either` 类型。

# 使用Either

# 使用ValidationNel

# 总结
