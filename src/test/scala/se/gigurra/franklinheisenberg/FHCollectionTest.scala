package se.gigurra.franklinheisenberg

import java.util.UUID

import org.scalatest._
import org.scalatest.mock._
import se.gigurra.franklin.{ItemAlreadyExists, ItemNotFound, WrongDataVersion, YeahReally}
import se.gigurra.franklinheisenberg.FHCollection._
import se.gigurra.heisenberg.MapData.SourceData
import se.gigurra.heisenberg.{Parsed, Schema}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Try}

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
    val partyMembers = required[Seq[String]]("partyMembers")

    def apply(name: String, partyMembers: Seq[String] = Seq.empty): OuterType = marshal(
      this.name -> name,
      this.partyMembers -> partyMembers
    )
  }

  case class OuterType private(source: SourceData) extends Parsed[OuterType] {
    def schema = OuterType

    val name = parse(schema.name)
    val partyMembers = parse(schema.partyMembers)
  }

  val provider: FHStore = FranklinHeisenberg.loadInMemory()
  //val provider: FHStore = FranklinHeisenberg.loadMongo()

  val collection = provider.getOrCreate[OuterType, OuterType.type]("test_fhcollection", OuterType)

  override def beforeEach(): Unit = {
    collection.wipeItems().yesImSure().await()
    collection.wipeIndices().yesImSure().await()
  }

  override def afterAll(): Unit = {
    provider.close()
  }

  "FHCollection" should {

    "be created" in {
      collection should not be null
    }

    "create and delete indices" in {
      collection.createIndex(_.name, unique = true).await()
      collection.fieldIndices.await() shouldBe Seq(OuterType.name)
      collection.deleteIndex(_.name)(YeahReally()).await()
      collection.fieldIndices.await() shouldBe Seq()

      collection.createIndex(_.partyMembers, unique = true).await()
      collection.createIndex(_.name, unique = true).await()
      collection.createUniqueIndex(OuterType.name, unique = true).await()
      collection.fieldIndices.await() shouldBe Seq(OuterType.name, OuterType.partyMembers)

      collection.indices.await().map(collection.deleteIndex(_)(YeahReally())).await()
      collection.fieldIndices.await() shouldBe Seq()
    }

    "add and find some partyMembers" in {

      collection.createIndex(_.name, unique = true).await()
      collection.createIndex(_.partyMembers, unique = true).await()

      val a1 = OuterType("a", Seq("x", "y", "z"))
      val a2 = OuterType("a", Seq("X", "Y", "Z"))
      collection.create(a1).await()
      val resulta2 = Try(collection.create(a2).await())
      resulta2 shouldBe an[Failure[_]]
      resulta2.failed.get shouldBe an[ItemAlreadyExists]

      val b1 = OuterType("b", Seq("å", "ä", "ö"))
      val b2 = OuterType("b", Seq("X", "Y", "Z"))
      collection.create(b1).await()
      val resultb2 = Try(collection.create(b2).await())
      resultb2 shouldBe an[Failure[_]]
      resultb2.failed.get shouldBe an[ItemAlreadyExists]

      val storedItems: Seq[Versioned[OuterType]] = collection.where().find.await()
      storedItems should contain(Versioned(a1, version = 1L))
      storedItems should contain(Versioned(b1, version = 1L))
      storedItems.size shouldBe 2

      collection.where(_.name --> "a").find.await().head.data shouldBe a1
      collection.where(_.partyMembers --> "y").find.await().contains(Versioned(a1, 1L)) shouldBe true
      collection.where(_.partyMembers --> "å").find.await().contains(Versioned(b1, 1L)) shouldBe true

      collection.where(_.partyMembers --> "å").find.await().size shouldBe 1
      collection.where(_.partyMembers --> "x").find.await().size shouldBe 1
    }

    "Update values" in {

      collection.createIndex(_.name, unique = true).await()

      val a = OuterType("a", Seq("x", "y", "z"))
      val a2 = OuterType("a", Seq("x", "y", "z", "LALALA"))
      val b = OuterType("b", Seq("X", "Y", "Z"))
      collection.create(a).await()
      collection.create(b).await()

      collection.where(a).update(a2, expectPrevVersion = 1L).await()
      Try(collection.where(a).update(a2, expectPrevVersion = 1L).await()) shouldBe an[Failure[_]]
      Try(collection.where(_.name --> "a").update(a2, expectPrevVersion = 1L).await()).failed.get shouldBe an[WrongDataVersion]
      Try(collection.where(_.name --> "ax").update(a2, expectPrevVersion = 2L).await()).failed.get shouldBe an[ItemNotFound]


      collection.where().size.await() shouldBe 2
      collection.where(_.name --> "a").find.await().head.data shouldBe a2

    }

    "Append" in {

      collection.createIndex(_.name, unique = true).await()

      val a = OuterType("a", Seq("bob"))
      val b = OuterType("b", Seq("bob"))

      collection.create(a).await()
      collection.where(a).append(_.partyMembers --> Seq("j", "m", "r"), defaultValue = () => a).await()
      collection.where(b).append(_.partyMembers --> Seq("j", "m", "r"), defaultValue = () => b).await()
      collection.where(_.name --> a.name).find.await().head.data.partyMembers should contain allOf ("j", "m", "r", "bob")
      collection.where(_.name --> b.name).find.await().head.data.partyMembers should contain allOf ("j", "m", "r", "bob")

    }

    "findOrCreate" in {

      collection.createIndex(_.name, unique = true).await()

      val a1 = OuterType("a1", Seq("1"))
      val a1b = OuterType("a1", Seq("1b"))

      val a2 = OuterType("a2", Seq("2"))

      collection.where(a1).findOrCreate(ctor = () => a1).await()
      collection.where(a1).findOrCreate(ctor = () => a1b).await()
      collection.where(a1).size.await() shouldBe 1
      collection.where().size.await() shouldBe 1
      collection.where(a1).find.await().head.data shouldBe a1

      collection.where(a2).findOrCreate(ctor = () => a2).await()
      collection.where(a2).size.await() shouldBe 1
      collection.where(a2).find.await().head.data shouldBe a2

    }

  }


  def randomId: String = UUID.randomUUID().toString
}
