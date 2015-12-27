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
 val foo = required[String]("a", default = "foo_default")
 val bar = optional[Int]("b")
}

case class MyType private(source: Map[String, Any]) extends Parsed[MyType.type] {
 val foo = parse(schema.foo)
 val bar = parse(schema.bar)
}
```

Create the collection:

```scala
val collection: FHCollection = provider.getOrCreate("test_fhcollection", MyType)

```
