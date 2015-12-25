package se.gigurra.franklinheisenberg

import java.util.UUID

import org.scalatest._
import org.scalatest.mock._
import se.gigurra.franklin.YeahReally
import se.gigurra.heisenberg.MapData.SourceData
import se.gigurra.heisenberg.{Parsed, Schema}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

class FHCollectionTest
  extends WordSpec
  with MockitoSugar
  with Matchers
  with OneInstancePerTest
  with BeforeAndAfterAll
  with BeforeAndAfterEach {

  implicit class RichFuture[T](f: Future[T]) {
    def await(): T = Await.result(f, Duration.Inf)
  }

  implicit class RichFuture2[T](f: Seq[Future[T]]) {
    def await(): Seq[T] = Await.result(Future.sequence(f), Duration.Inf)
  }

  object OuterType extends Schema[OuterType] {
    val name = required[String]("name")
    val items = required[Seq[String]]("items")

    def apply(name: String, items: Seq[String] = Seq.empty): OuterType = marshal (
      this.name -> name,
      this.items -> items
    )
  }

  case class OuterType private(source: SourceData) extends Parsed[OuterType] {
    def schema = OuterType
    val name = parse(schema.name)
    val items = parse(schema.items)
  }

  val provider: FHStore = FranklinHeisenberg.loadInMemory()
  //val provider: FHStore = FranklinHeisenberg.loadMongo()

  val collection = provider.getOrCreate("test_fhcollection", OuterType, () => OuterType.apply("SomeName", Seq.empty))

  override def beforeEach(): Unit = {
    collection.wipeItems().yesImSure().await()
    collection.wipeIndices().yesImSure().await()
  }

  override def afterEach(): Unit = {
    provider.close()
  }

  "FHCollection" should {

    "be created" in {
      collection should not be null
    }

    "create and delete indices" in {
      collection.createUniqueIndex(_.name).await()
      collection.fieldIndices.await() shouldBe Seq(OuterType.name)
      collection.deleteIndex(_.name)(YeahReally()).await()
      collection.fieldIndices.await() shouldBe Seq()

      collection.createUniqueIndex(_.items).await()
      collection.createUniqueIndex(_.name).await()
      collection.createUniqueIndex(OuterType.name).await()
      collection.fieldIndices.await() shouldBe Seq(OuterType.name, OuterType.items)

      collection.indices.await().map(collection.deleteIndex(_)(YeahReally())).await()
      collection.fieldIndices.await() shouldBe Seq()
    }

    "have some indices" in {
    }

    "add some items" in {
    }

    "find some items" in {
    }

    "Update existing values" in {
    }

    "Update non-existing values" in {
    }

    "Index on arrays / find on index elements /Append" in {
    }

    "LoadOrCreate" in {
    }

  }


  def randomId: String = UUID.randomUUID().toString
}
