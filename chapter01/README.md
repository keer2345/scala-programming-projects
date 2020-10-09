# Chapter 01: Writing Your First Program 

Scala 始于 2001 年，顾名思义，它是可扩展（Scalable）的语言，可以将其用于小的脚本或者大型企业应用。

Scala 在分布式可扩展系统和大数据尤其出色。许多优秀的开源软件是用 Scala 开发的，例如 Apache Spark,Apache Kafka, Finagle, Akka.

Scala 并非 Java 的扩展，而是可以相互操作的。可以从 Scala 调用 Java，反之亦可。Scala 也可编译成 JavaScript，从而在浏览器中运行 Scala 代码。

Scala 混合了面向对象和函数式编程的语法，并且是静态类型的语言。

本章将学习：

- 设置环境
- 使用基本特性
- 在终端运行 Scala
- 使用 Scala Console 和 Worksheet
- 创建第一个项目

# 设置环境

Scala 程序被编译成 Java 字节码。因此，我们需要 JVM。还需要使用文编编辑器和 *Simple Build Tool (SBT)* 。最广泛而且专业的 IDE 是 JetBrains 的 IntelliJ IDEA，它提供了语法高亮、自动补全、代码导航，并集成了 SBT 等等。东冉，也可以使用 Eclipse, Emacs, Vim, Atom, Sublime, VSCode。

# 基础特性

## SBT synchronization
创建项目：
```
sbt new scala/scala-seed.g8
```

`build.sbt`:
```scala
import Dependencies._

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "scala_fundamentals",
    libraryDependencies += scalaTest % Test
  )
```
测试和运行：
```
sbt test

sbt run
```
## SBT太慢了？
如果太慢的话，编辑 `~/.sbt/repositories`:
```
[repositories]
local
aliyun: http://maven.aliyun.com/nexus/content/groups/public
typesafe-ivy-releases: http://repo.typesafe.com/typesafe/ivy-releases/, [organization]/[module]/[revision]/[type]s/[artifact](-[classifier]).[ext], bootOnly
sonatype-oss-releases
maven-central
sonatype-oss-snapshots
```

如果卡着不动，可以参考[这里](https://cloud.tencent.com/developer/article/1016803):
```shell
rm -rf ~/.sbt/boot/sbt.boot.lock
rm -rf ~/.ivy2/.sbt.ivy.lock
rm -rf ~/.ivy2/.sbt.cache.lock
```

# 在终端进行Scala工作
## 在终端运行Scala
```scala
~ scala
Welcome to Scala 2.13.3 (OpenJDK 64-Bit Server VM, Java 14.0.1).
Type in expressions for evaluation. Or try :help.

scala> 1 + 1
val res0: Int = 2
```
### 声明变量
```scala
scala> val x = 1 + 1
val x: Int = 2

scala> val y: Int = 1 + 1
val y: Int = 2

scala> var x = 1
var x: Int = 1
```

### 类型
- Int
- Boolean
- Douible
- String

类型决定了数据操作的可行性。例如，可以使用 `+` 操作 `Int` 和 `String` 类型，但是不能操作 `Boolean` 类型。
```scala
scala> val str = "Hello" + "World"
val str: String = HelloWorld

scala> val i = 1 + 1
val i: Int = 2

scala> val b = true + false
                      ^
       error: type mismatch;
        found   : Boolean(false)
        required: String
```

Scala 一个重要的特性是静态类型，意味着变量或者表达式的类型在编译时就知道了，编译器还将检查您是否调用了对此类型无效的操作或函数，这有助于极大地减少运行时出现的 bug 。

> IntelliJ IDEA 可以自动地添加类型。

### 声明和调用函数

```scala
scala> def presentation(name: String, age: Int): String =
     |   "Hello, my name is " + name + ". I am " + age + " years old."
def presentation(name: String, age: Int): String

scala> presentation(name="Bob", age=25)
val res4: String = Hello, my name is Bob. I am 25 years old.

scala> presentation(age=25, name="Tom")
val res6: String = Hello, my name is Tom. I am 25 years old.
```

### 副作用

函数或者表达式的副作用是指当状态被修改、或者有来自外部的举动。例如，在终端打印字符串，写入文件，以及修改 `var` 变量…… 都存在副作用。

在 Scala 里，所有表达式都有类型。执行副作用的类型属于 `Unit`，类型 `Unit` 的值只能是 `()`：
```scala
scala> val x = println("hello")
hello
val x: Unit = ()

scala> println(x)
()

scala> def  printName(name: String): Unit = println(name)
def printName(name: String): Unit

scala> val y = {
     |   var a = 1
     |   a = a + 1
     | }
val y: Unit = ()

scala> val z = ()
val z: Unit = ()
```

纯函数是指返回值只依赖于它的参数，并且没有副作用。

> 最佳实践：当无参数的函数存在副作用，应该声明它并用空括号 `()` 来表示。

下面这个例子存在副作用：
```scala
scala> def helloWorld(): Unit = println("Hello World")
def helloWorld(): Unit

scala> helloWorld()
Hello World

scala> def helloWorldPurl: String = "Hello world"
def helloWorldPurl: String

scala> val x = helloWorldPurl
val x: String = Hello world
```

### if-else表达式
```scala
scala> def agePeriod(age: Int): String = {
     |   if(age >= 65)
     |     "elderly"
     |   else if (age >= 40 && age < 65)
     |     "middle aged"
     |   else if (age >= 18 && age < 40)
     |     "young adult"
     |   else
     |     "child"
     | }
def agePeriod(age: Int): String
```
如果子表达式有不同类型，编译器将指向他们的父类型：
```scala
scala> val ifElseWiden = if(true)2: Int else 2.0: Double
val ifElseWiden: Double = 2.0

scala> val ifElseSuperType = if(true) 2 else 2.0
val ifElseSuperType: Double = 2.0

scala> val ifElseSuperType = if(true) 2 else "2"
val ifElseSuperType: Any = 2
```
```scala
scala> val ifWithoutElse = if (true) 2
val ifWithoutElse: AnyVal = 2

scala> val ifWithoutElseExpanded = if (true) 2: Int else(): Unit
val ifWithoutElseExpanded: AnyVal = 2

scala> def sideEffectingFunction(): Unit = if (true) println("hello world")
def sideEffectingFunction(): Unit
```
## 类
`class` 是创建特定类型对象的模板，当我们想获取一个值的类型，可以通过类名用 `new` 实例化对象：
```scala
scala> class Robot
class Robot

scala> val nao = new Robot
val nao: Robot = Robot@7e2c3f31
```
操作符 `eq` 可以校验两个引用是否相等，如果相等，意味着在内存中是相同的：
```scala
scala> val johnny5 = new Robot
val johnny5: Robot = Robot@e5522a5

scala> nao eq johnny5
val res17: Boolean = false
```

类可以有 0 个或者多个成员（members），成员也可以试：
- 属性（attribute），也成为字段（field）。它是表示类的每个实例的变量。
- 方法（method）。这是一个可以读写实例属性的函数，它也可以有其他参数。

下面这个类定义了一些成员：
```scala
scala> class Rectangle(width: Int, height: Int) {
     |   val area: Int = width * height
     |   def scale(factor: Int): Rectangle = new Rectangle(width * factor, height * factor)
     | }
class Rectangle
```
属性被声明在小括号 `()` 内：他们是构造参数（**constructor arguments**），意思是类实例化成新的对象时，它们必须被指定值。其他成员必须是定义在大括号 `{}` 内。在这里例子在宏，我们定义了四个成员：
- 两个构造参数的属性：`width` 和 `height`。
- 一个属性 `area`，它由实例化的构造参数决定。
- 一个方法 `scale`，利用属性来创建类 `Rectangle` 的新实例。
```scala
scala> val square = new Rectangle(2, 2)
val square: Rectangle = Rectangle@7ec8e029

scala> square.area
val res18: Int = 4

scala> val square2 = square.scale(3)
val square2: Rectangle = Rectangle@1b22a77c

scala> square2.area
val res19: Int = 36

scala> square.width
              ^
       error: value width is not a member of Rectangle
```

我们可以访问成员 `area` 和 `scale`，但是不能访问 `width`，为什么呢？

因为默认情况下，构造参数不能从外部访问，它们是私有（private）的，只能实力内部的其他成员访问。如果想要访问构造参数，需要加上前缀 `val`：
```scala
scala> class Rectangle(val width: Int, val height: Int) {
     |   val area: Int = width * height
     |   def scale(factor: Int): Rectangle = new Rectangle(width * factor, height * factor)
     | }
class Rectangle

scala> val rect = new Rectangle(3, 2)
val rect: Rectangle = Rectangle@2f580c1

scala> rect.width
val res22: Int = 3

scala> rect.height
val res24: Int = 2
```
现在，我们可以访问构造参数了。

注意，我们也可以使用 `var` 来替代 `val`，这样将可以改变构造参数。但是在函数编程中，我们要避免变量可变。

`var` 属性应该小心谨慎地使用。如果需要修改属性，最好是返回新的实例，就像前面的 `Rectangle.scale` 方法。

### 类的继承
```scala
scala> class Shape(val x: Int, val y: Int) {
     |   val isAtOrigin: Boolean = x == 0 && y == 0
     | }
class Shape

scala> class Rectangle(x: Int, y: Int, val width: Int, val height: Int) extends Shape(x, y)
class Rectangle

scala> class Square(x: Int, y: Int, width: Int) extends Rectangle(x, y, width, width)
class Square

scala> class Circle(x: Int, y: Int, radius: Int) extends Shape(x, y)
class Circle
```
```scala
scala> val rect = new Rectangle(x = 0, y = 3, width = 4, height = 5)
val rect: Rectangle = Rectangle@5ebe41ef

scala> rect.x
val res33: Int = 0

scala> rect.y
val res34: Int = 3

scala> rect.isAtOrigin
val res36: Boolean = false

scala> rect.width
val res38: Int = 4

scala> rect.height
val res40: Int = 5
```
`Rectangle` 和 `Circle` 是 `Shape` 的子类，继承了 `Shape` 的 `x`, `y` 和 `isAtOrigin`。

当声明了子类，就需要通过父类的构造参数。类似 `Shape` 声明了两个构造参数 `x` 和 `y`，我们需要使用 `extends Shape(x, y)` 来声明继承，`x` 和 `y` 也是 `Rectangle` 的构造参数。

注意在子类中，`x` 和 `y` 的声明中没有 `val`，如果我们使用了 `val`，将是 public 的可读属性，随之而来的问题是 `Shape` 也有公共可读的 `x` 和 `y`。这种情况下，编译器将抛出冲突错误。

### 子类参数
考虑两个类，`A` 和 `B` 的关系为 `B extends A`。

当声明类型为 `A` 的变量，可以指派给实例 `B`：
```scala
val a: A = new B
```
换句话说，如果声明了类型为 `B` 的变量，不能指派给实例 `A`。比如：
```scala
scala> val shape: Shape = new Rectangle(x = 0, y = 3, width = 3, height = 2)
val shape: Shape = Rectangle@7fb91def

scala> val rectangle: Rectangle = new Shape(x = 0, y = 3)
                                  ^
       error: type mismatch;
        found   : Shape
        required: Rectangle
```

### 重写方法
我们可以重写父类成员的方法：
```scala
scala> class Shape(val x: Int, val y: Int) {
     |   def description: String = s"Shape at (" + x + ", " + y + ")"
     | }
class Shape

scala> class Rectangle(x: Int, y: Int, val width: Int, val height: Int) extends Shape(x, y) {
     |   override def description: String = {
     |     super.description + s" - Rectangle " + width + " * " + height
     |   }
     | }
class Rectangle

scala> val rect = new Rectangle(x=0, y=3, width=4, height=5)
val rect: Rectangle = Rectangle@105e4da1

scala> rect.description
val res42: String = Shape at (0, 3) - Rectangle 4 * 5
```

关键字 `super` 表示调用父类成员，`this` 表示调用当前类的成员：
```class
scala> :paste
// Entering paste mode (ctrl-D to finish)

class Rectangle(x: Int, y: Int, val width: Int, val height: Int)
     extends Shape(x, y) {
     override def description: String = {
       super.description + s" - Rectangle " + width + " * " + height
     }
     def descThis: String = this.description
     def descSuper: String = super.description
}



// Exiting paste mode, now interpreting.

class Rectangle

scala> val rect = new Rectangle(x=0, y=3, width=5, height = 7)
val rect: Rectangle = Rectangle@7dfdf6a7

scala> rect.description
val res45: String = Shape at (0, 3) - Rectangle 5 * 7

scala> rect.descThis
val res46: String = Shape at (0, 3) - Rectangle 5 * 7

scala> rect.descSuper
val res49: String = Shape at (0, 3)
```
### 抽象类
抽象类是指包含有多个抽象成员的类。抽象成员是指没有具体实现的属性或方法。我们不可以实例化抽象类，必须创建它的子类来实现所有的抽象成员。
```scala
scala> abstract class Shape(val x: Int, val y: Int) {
     |   val area: Double
     |   def description: String
     | }
class Shape

scala> class Rectangle(x: Int, y: Int, val width: Int, val height: Int) extends Shape(x, y) {
     |   val area: Double = width * height
     |   def description: String = "Rectangle " +  width + " * " + height
     | }
class Rectangle
```

类 `Shape` 是抽象的，不能直接实例化，必须创建子类 `Rectangle` 或者其他子类来实例化。`Shape` 定义了两个成员 `x` 和 `y`，还有两个抽象成员 `area` 和 `description`，子类 `Rectangle` 实现了这两个抽象成员。

> 当实现抽象成员的时候，我们可以使用前缀 `override`，当没有必要，以保持代码的简洁。当然，如果随后在父类中实现了抽象方法，编译器将帮助你找到所有的实现它的子类，如果使用了 `override` 将不会执行此操作。


### 特质Trait
特质很像抽象类，它可以说明多个抽象或者其他成员并可继承。但不能实例化。不同的是抽象类只能继承一个，而特质可以混入（**mixin**）一个到多个。 另外，特质不能带构造参数。

``` scala
scala> trait Description {
     |   def description: String
     | }
trait Description

scala> trait Coordinates extends Description {
     |   def x: Int
     |   def y: Int
     |
     |   def description: String =
     |     "Coordinates ( " + x + ", " + y + " )"
     | }
trait Coordinates

scala> trait Area {
     |   def area: Double
     | }
trait Area

scala> class Rectangle(val x: Int,
     |   val y: Int,
     |   val width: Int,
     |   val height: Int) extends Coordinates with Description with Area {
     |
     |   val area: Double = width * height
     |   override def description: String =
     |     super.description + " - Rectangle " + width + " * " + height
     | }
class Rectangle

scala> val rect = new Rectangle(0, 3, 4, 5)
val rect: Rectangle = Rectangle@7d0f3227

scala> rect.description
val res51: String = Coordinates ( 0, 3 ) - Rectangle 4 * 5
```

类 `Rectangle` 混入了特质 `Coordinates`, `Description` 和 `Area`，我们需要先使用 `extends` 继承 `trait` 或 `class`，再用关键词 `with` 混入其他他特质。

注意，`Coordinates` 也混入了特质 `Description`，并提供了默认实现。当我们有一个 `Shape` 类时，我们重载 `Rectangle` 中的实现，通过 `super.description` 调用 `trait Coordinates` 中的 `description`。

另一个有意思的地方是，你可以使用 `val` 来实现 `trait Area` 的抽象方法，`Area` 中我们定义了 `def area: Double`，并在 `Rectangle` 中用 `val area: Double` 实现它。这是用 `def` 定义抽象成员的良好实践。这样，特质的定义者这样决定使用方法还是变量来定义它。

### Scala类的等级
所有的 Scala 类型都是继承内建的 `Any` 类型，它是根类型，有两个直接的子类型：
- `AnyVal`，它是所有的值类型的根类，这些类型是 JVM 中的原始类型。
- `AnyRef`，对象类型的根类，别名是 `java.lang.Object`。
- 变量类型为 `AnyVal` 直接包含值，相对于 `AnyRef` 类型的变量包含存储在内存的对象地址。

当我们定义了新的类，它直接继承 `AnyRef`，拥有了 `Object` 中默认方法的实现。重要的方法如下：
- `def toString: String` 返回对象的字符串表示，调用并打印 `println`，默认的实现返回类的名字和对象的内存地址。
- `def equals(obj: Object): Boolean` 如果对象与另一个对象相等，则返回 `true`，否则返回 `false`。该方法相对于 `==`。
- `def hashCode: Int` 无论将对象放在 `Set` 还是将它当做关键词放在 `Map`。默认的是实现是基于对象的地址。如果想要更好的在 `Set` 或 `Map` 分配数据可以重载方法，可以提升集合的性能。然而，如果这样做，必须确保 `hashCode` 与 `equal` 一致，如果两个对象是相等（equal）的，他们的 `hashCode` 必须也是相等的。

重写这些方法是非常乏味的。幸运的是，Scala 提供了特别的构造，称为 `case class`，它能为我们自动重载方法。
### Case Class

Scala 里，通过 case class 定义了很多数据结构。`case class` 有多个不可变的属性，并提供了几个内建函数：

``` scala
scala> case class Person(name: String, age: Int)
class Person

scala> val mikaelNew = new Person("Mikael", 41)
val mikaelNew: Person = Person(Mikael,41)

scala> val mikael = Person("Mikael",41)
val mikael: Person = Person(Mikael,41)

scala> mikael == mikaelNew
val res55: Boolean = true

scala> val name = mikael.name
val name: String = Mikael

scala> val nicolas = mikael.copy(name = "Nicolas")
val nicolas: Person = Person(Nicolas,41)

scala> nicolas.toString()
val res58: String = Person(Nicolas,41)
```

### 伴生对象
类可以拥有伴生对象，必须声明在和类同一个文件中，在类的伴生名称前使用 `object` 关键词。伴生对象是单例的 —— 在 JVM 中该对象只有一个实例。它由自己的类型并且没有伴生类的实例。

对象定义静态函数或者值与类是紧密关联伴生的，如果你熟悉 Java，它就类似关键词 `static`：在 Scala 里，所有的静态成员被声明在伴生对象内部。

在伴生对象中的一些函数有着特别的意义：`apply` 函数是类的构造器，通常可以省略它的名称 `apply`：

``` scala
case class City(name: String, urbanArea: Int)
object City {
     val London = City("London", 1738)
     val Lausanne = City("Lausanne", 41)
}

case class Person(firstName: String, lastName: String, city: City)
object Person {
    def apply(fullName: String, city: City): Person = {
    val splitted = fullName.split(" ")
    new Person(firstName = splitted(0), lastName = splitted(1), city = city)
    }
}

// Uses the default apply method
val m1 = Person("Mikael", "Valot", City.London)

// Call apply with fullName
val m2 = Person("Mikael Valot", City.London)

// We can omit 'apply'
val n = Person.apply("Nicolas Jorand", City.Lausanne)
```


# 创建第一个项目
## 创建项目
我们可以通过 IntelliJ IDEA 创建 Scala 项目，也可以通过 SBT 创建：

```
> sbt new scala/scala-seed.g8
[warn] insecure HTTP request is deprecated 'http://maven.aliyun.com/nexus/content/groups/public/'; switch to HTTPS or opt-in as ("osc" at "http://maven.aliyun.com/nexus/content/groups/public/").withAllowInsecureProtocol(true)

A minimal Scala project. 

name [Scala Seed Project]: Example

Template applied in ~/example
```

```
> cd example
> tree
.
├── build.sbt
├── project
│   ├── Dependencies.scala
│   └── build.properties
└── src
    ├── main
    │   └── scala
    │       └── example
    │           └── Hello.scala
    └── test
        └── scala
            └── example
                └── HelloSpec.scala
```

## 创建Main对象
`src/main/scala/Main.scala`:

``` scala
object Main extends App {}
```
运行项目：
```
sbt run
```

## 编写第一个测试单元
TDD 在书写效率上很强大，它有三个规则：

1. 先不写任何生产代码，除非它在测试时发生了错误。
1. 任何测试单元都不允许失败。
1. 测试单元失败的话不允许再写更多的生产代码。

Scala 的测试框架有很多，我们在这里选择简单的 [ScalaTest](https://www.scalatest.org/) 。

添加 ScalaTest 库`build.sbt`:
``` scala
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0" % "test"
```

创建 `src/test/scala/MainSpec`:

``` scala
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class MainSpec extends AnyWordSpec {
  "A Person" when {
    "be instantiated" should {
      "with a age and name" in {
        val john = Person(firstName = "John", lastName = "Smith", 42)
        john.firstName should be("John")
        john.lastName should be("Smith")
        john.age should be(42)
      }
    }
  }
}
```
`src/main/scala/Person.class`:

``` scala
case class Person(firstName: String, lastName: String, age: Int) {}
```
运行测试：

```
sbt test
```
正常的话会出现类似如下测试结果：
```
... ...

[info] MainSpec:
[info] A Person
[info]   when be instantiated
[info]   - should with a age and name
[info] Run completed in 586 milliseconds.
[info] Total number of tests run: 1
[info] Suites: completed 1, aborted 0
[info] Tests: succeeded 1, failed 0, canceled 0, ignored 0, pending 0
[info] All tests passed.
```
## 实现其他特性
刚才，我们已经可以很好地表示一个人的姓名和年龄了，我们继续添加测试：
`src/test/scala/MainSpec.scala`:

``` scala
    "companion object" should {
      val (akira, peter, nick) =
        (
          Person(firstName = "Akira", lastName = "Sakura", age = 12),
          Person(firstName = "Peter", lastName = "Müller", age = 34),
          Person(firstName = "Nick", lastName = "Tagart", age = 52)
        )
      "return a list of adult person" in {
        val ref = List(akira, peter, nick)
        Person.filterAdult(ref) should be(List(peter, nick))
      }
    }
```
`src/main/scala/Person.scala`:

``` scala
case class Person(firstName: String, lastName: String, age: Int) {
  def description =
    s"$firstName $lastName is $age ${if (age < 1) "year" else "years"} old"
}

object Person {
  def filterAdult(persons: List[Person]): List[Person] =
    persons.filter(_.age >= 18)
}
```

## 实现Main方法
# 总结
