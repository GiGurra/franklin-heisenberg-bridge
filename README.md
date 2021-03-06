# Franklin-Heisenberg-bridge
Connecting [Franklin](https://github.com/GiGurra/franklin) with [Heisenberg](https://github.com/GiGurra/heisenberg)..

* Franklin handles dynamic data storage and retrieval
* Heisenberg handles dynamic<->static data interpretation.

Franklin-Heisenberg-bridge provides a Heisenberg typed interface to Franklin storage, easy - right? :).

## Examples

### What you need

In your build.sbt:
```sbt
libraryDependencies += "com.github.gigurra" %% "franklin-heisenberg-bridge" % "0.1.20"
```
In your code:
```scala
import com.github.gigurra.franklinheisenberg._
import com.github.gigurra.franklinheisenberg.FHCollection._

val provider: FHStore = FranklinHeisenberg.loadInMemory()
// FranklinHeisenberg.loadMongo(database: String = "local", nodes: Seq[String] = Seq("127.0.0.1:27017"), codec: BsonCodec = DefaultBsonCodec)

```

### Create a collection

Note: Make sure you understand what [Franklin](https://github.com/GiGurra/franklin) and [Heisenberg](https://github.com/GiGurra/heisenberg) are first!

```scala

// Given some Heisenberg type
case class MyHeisenbergType... 
object MyHeisenbergType extends Schema[MyHeisenbergType] ..

// We can create a collection of type FHCollection[MyHeisenbergType, MyHeisenbergType.type]
val collection = provider.getOrCreate("test_collection", MyHeisenbergType)

```


### Create some indices + store & load some objects

```scala

// Suppose we have a Schema that looks something like this:
object MyHeisenbergType extends Schema[MyHeisenbergType] {
 val name = required[String](..)
 val items = required[Seq[String]](..)
}

// We can create the indices directly on fields
val op_i1: Future[Unit] = collection.createIndex(_.name, unique = true)
val op_i2: Future[Unit] = collection.createIndex(_.items, unique = true)

// await..

// Suppose now we have some objects we want to store
val a: MyHeisenbergType = ..
val b: MyHeisenbergType = ..

// We call .create just like in franklin, except that now we have static typing!
val op1: Future[Unit] = collection.create(a)
val op2: Future[Unit] = collection.create(b)

// await..

// Find on .name member
val aBack: Future[Option[Versioned[MyHeisenbergType]]] = collection.where(_.name --> a.name).findOne
val bBack: Future[Option[Versioned[MyHeisenbergType]]] = collection.where(b).findOne
val allItems: Future[Seq[Versioned[MyHeisenbergType]]] = collection.where().findAll

// Find all MyHeisenbergType objects with "Bob" in their .items field
val foo = collection.where(_.items --> "Bob").find

// Future[Option[Versioned[MyType..]]] - Wow thats a lot of wrappers!!!
// 

```

### What's this Versioned[..] thing?

* Make sure you understand Franklin's versioning.
* Versioned[T] is just a wrapper for any type T with a version number:

```scala
case class Versioned[T](data: T, version: Long)

// which also works as a decorator with implicit Versioned[T] => T conversion
```


### That's a lot of wrappers

* Future[Option[Versioned[MyType..]]]  :(

Let's make that a bit more workable

```scala
def makeMyNameLonger(myPreviousName: String): .. = {
 for {
   // Check that I actually exist. Below binds a Versioned[MyType] -> c
   c <- collection.where(_.name --> myPreviousName).findOne.map(_.getOrElse(..))
   // Update my name
   _ <- collection.where(c).update(c.withNewName(myPreviousName+"longer!"), expectVersion = c.version)
 } yield {
  Http.Response(Ok, "I did my job!")
 }
}

```


### For more examples ..

Have a look at [the (incomplete) tests](https://github.com/GiGurra/franklin-heisenberg-bridge/blob/master/src/test/scala/se/gigurra/franklinheisenberg/FHCollectionTest.scala)

