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
}

case class MyType private(source: Map[String, Any]) extends Parsed[MyType.type] {
 val name = parse(schema.name)
 val partyMembers = parse(schema.partyMembers)
}
```

Create the collection:

```scala
val collection: FHCollection = provider.getOrCreate("test_fhcollection", MyType)

```


### Create some indices

```scala
val op1: Future[Unit] = collection.createIndex(_.name, unique = true)
val op2: Future[Unit] = collection.createIndex(_.partyMembers, unique = true)
```
