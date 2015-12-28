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

Note: Make sure you understand what heisenberg is first (see [heisenberg](https://github.com/GiGurra/heisenberg)):

```scala

// Defined earlier
case class MyHeisenbergType... 
object MyHeisenbergType extends Schema[MyHeisenbergType] {
 val name = required[String](..)
 val members = required[Seq[String]](..)
 ..
}

// A FHCollection[MyHeisenbergType, MyHeisenbergType.type]
val collection = provider.getOrCreate("test_collection", MyHeisenbergType)

```


### Create some indices

```scala
val op1: Future[Unit] = collection.createIndex(_.name, unique = true)
val op2: Future[Unit] = collection.createIndex(_.members, unique = true)
```


### Store and load some data

```scala
collection.createIndex(_.name, unique = true).await()
collection.createIndex(_.members, unique = true).await()

val a: MyHeisenbergType = ..
val b: MyHeisenbergType = ..

val op1: Future[Unit] = collection.create(a)
val op2: Future[Unit] = collection.create(b)

// await..

// Find on .name member
val aBack: Future[Option[Versioned[MyHeisenbergType]]] = collection.where(_.name --> a.name).findOne
val bBack: Future[Option[Versioned[MyHeisenbergType]]] = collection.where(b).findOne
val allItems: Future[Seq[Versioned[MyHeisenbergType]]] = collection.where().findAll

// Find all MyHeisenbergType objects with "Bob" in their .members
val foo = collection.where(_.members --> "Bob").find

```
