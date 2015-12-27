# Franklin-Heisenberg-bridge
Connecting [Franklin](https://github.com/GiGurra/franklin) with [Heisenberg](https://github.com/GiGurra/heisenberg)..

* Franklin handles dynamic data storage and retrieval
* Heisenberg handles dynamic data interpretation/mapping.

Franklin-Heisenberg-bridge provides a Heisenberg typed interface to Franklin storage, easy - right? :).

## Examples

### What you need

In your build.sbt:
```sbt
.dependsOn(uri("git://github.com/GiGurra/franklin-heisenberg-bridge.git#0.1.9"))
```
In your code:
```scala
import se.gigurra.franklinheisenberg._
import se.gigurra.franklinheisenberg.FHCollection._

val provider: FHStore = FranklinHeisenberg.loadInMemory()
// FranklinHeisenberg.loadMongo(database: String = "local", nodes: Seq[String] = Seq("127.0.0.1:27017"))

```

### Create a collection

Based on Heisenberg type MyType (see [heisenberg](https://github.com/GiGurra/heisenberg)):

```scala
object MyType extends Schema[MyType] {
 val name = required[String]("name", default = "foo_default")
 val partyMembers = required[Seq[String]]("partyMembers", default = Seq.empty)
 
 def apply(name: String, partyMembers: Seq[String] = Seq.empty): OuterType = marshal(
  this.name -> name,
  this.partyMembers -> partyMembers
 )
}

case class MyType private(source: Map[String, Any]) extends Parsed[MyType.type] {
 val name = parse(schema.name)
 val partyMembers = parse(schema.partyMembers)
}
```

Create the collection:

```scala
val collection: FHCollection[MyType, MyType.type] = provider.getOrCreate("test_fhcollection", MyType)

```


### Create some indices

```scala
val op1: Future[Unit] = collection.createIndex(_.name, unique = true)
val op2: Future[Unit] = collection.createIndex(_.partyMembers, unique = true)
```


### Store and load some data

```scala
collection.createIndex(_.name, unique = true).await()
collection.createIndex(_.partyMembers, unique = true).await()

val a1 = MyType("a", Seq("x", "y", "z"))
val a2 = MyType("a", Seq("X", "Y", "Z"))
collection.create(a1).await()
val resulta2 = Try(collection.create(a2).await())
resulta2 shouldBe an[Failure[_]]
resulta2.failed.get shouldBe an[ItemAlreadyExists]

val b1 = MyType("b", Seq("å", "ä", "ö"))
val b2 = MyType("b", Seq("X", "Y", "Z"))
collection.create(b1).await()
val resultb2 = Try(collection.create(b2).await())
resultb2 shouldBe an[Failure[_]]
resultb2.failed.get shouldBe an[ItemAlreadyExists]

val storedItems: Seq[Versioned[MyType]] = collection.where().find.await()
storedItems should contain(Versioned(a1, version = 1L))
storedItems should contain(Versioned(b1, version = 1L))
storedItems.size shouldBe 2

collection.where(_.name --> "a").find.await().head.data shouldBe a1
collection.where(_.partyMembers --> "y").find.await().contains(Versioned(a1, 1L)) shouldBe true
collection.where(_.partyMembers --> "å").find.await().contains(Versioned(b1, 1L)) shouldBe true

collection.where(_.partyMembers --> "å").find.await().size shouldBe 1
collection.where(_.partyMembers --> "x").find.await().size shouldBe 1

```
