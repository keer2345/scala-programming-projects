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
      "Get a human readable representation of the person" in {
        val paul = Person(firstName = "Paul", lastName = "Smith", age = 24)
        paul.description should be("Paul Smith is 24 years old")
      }
    }
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
  }
}
