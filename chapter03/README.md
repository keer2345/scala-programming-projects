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

# 确保透明引用

# 使用Option

# 使用Either

# 使用ValidationNel

# 总结
