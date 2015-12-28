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


### A more advanced example

Below is code taken from he valhalla-game project. It handles people leaving a party


```scala
private def leaveParty(self: SelfInParty, leave: LeaveCurrentParty): Future[Response] = {

  val party = self.party.map(_.leave(self.charName))

  for {
    _ <- emit.toParty(LeftParty(self.charName), self.party)
    _ <- if (party.isDisbanded) {
      for {
        _ <- parties.where(_.id --> party.id).delete(expectVersion = party.version)
        _ <- emit.toParty(PartyDisbanded(), self.party)
      } yield ()
    } else {
      parties.where(_.id --> party.id).update(party, expectVersion = party.version)
    }
  } yield {
    Responses.ok(s"You left the party :/")
  }

}

```
