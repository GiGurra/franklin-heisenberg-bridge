package se.gigurra.franklinheisenberg

import se.gigurra.franklin._
import se.gigurra.heisenberg._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.reflect.runtime.universe._

case class FHCollection[T <: Parsed[S] : WeakTypeTag, S <: Schema[T]](franklin: Collection, schema: S) {
  import FHCollection._

  val tag = weakTypeTag[T]

  def createIndex(fGetField: S => Field[_], unique: Boolean): Future[Unit] ={
    val field = fGetField(schema)
    franklin.createIndex(field.name, unique)
  }

  def createIndex(field: Field[_], unique: Boolean): Future[Unit] ={
    franklin.createIndex(field.name, unique)
  }

  def create(entity: T): Future[Unit] = {
    franklin.create(MapProducer.produce(entity))
  }

  case class where_impl (selector: Map[String, Any]) {

    def create(): Future[Unit] = franklin.create(selector)
    def size: Future[Int] = franklin.size(selector)
    def isEmpty: Future[Boolean] = franklin.isEmpty(selector)
    def nonEmpty: Future[Boolean] = franklin.nonEmpty(selector)
    def contains: Future[Boolean] = franklin.contains(selector)

    def find: Future[Seq[Versioned[T]]] = franklin.find(selector).map(parse)

    def update(entity: T, upsert: Boolean = false, expectPrevVersion: Long): Future[Unit] = {
      franklin.update(selector, MapProducer.produce(entity), upsert, expectPrevVersion)
    }

    def append(fGetFieldData: S => (String, Any), defaultValue: () => T): Future[Unit] = {
      val data = fGetFieldData(schema).asInstanceOf[(String, Seq[Any])]
      franklin.append(selector, () => MapProducer.produce(defaultValue()), Seq(data))
    }

    def delete(expectVersion: Long = -1L): Future[Unit] = franklin.deleteItem(selector, expectVersion)

    def findOrCreate(ctor: () => T): Future[Versioned[T]] = {
      franklin.loadOrCreate(selector, () => MapProducer.produce(ctor())).map(parse)
    }
  }

  def where_raw(selector: Map[String, Any]): where_impl = new where_impl(selector)
  def where_raw(statements: SelectStatement*): where_impl = where_raw(statements.toMap)
  def where[T2 <: Parsed[_] : WeakTypeTag](query: T2): where_impl = where_raw(query)
  def where(selectors: (S => SelectStatement)*): where_impl = {
    val statements = selectors.map(_.apply(schema))
    where_raw(statements:_*)
  }

  def find_raw(selector: Map[String, Any]): Future[Seq[Versioned[T]]] = new where_impl(selector).find
  def find_raw(statements: SelectStatement*): Future[Seq[Versioned[T]]] = find_raw(statements.toMap)
  def find[T2 <: Parsed[_] : WeakTypeTag](query: T2): Future[Seq[Versioned[T]]] = find_raw(query)
  def find(selectors: (S => SelectStatement)*): Future[Seq[Versioned[T]]] = {
    val statements = selectors.map(_.apply(schema))
    find_raw(statements:_*)
  }

  def deleteIndex(index: String)(yeahReally: YeahReally): Future[Unit] = franklin.deleteIndex(index)(yeahReally)
  def deleteIndex(field: Field[_])(yeahReally: YeahReally): Future[Unit] = deleteIndex(field.name)(yeahReally)
  def deleteIndex(fGetField: S => Field[_])(yeahReally: YeahReally): Future[Unit] = deleteIndex(fGetField(schema))(yeahReally)

  def indices: Future[Seq[String]] = franklin.indices

  def wipeItems(): ItemsWiper = franklin.wipeItems()
  def wipeIndices(): IndicesWiper = franklin.wipeIndices()


  private implicit val parser = schema.parser
  private def parse(items: Seq[Item]): Seq[Versioned[T]] = items.map(parse)
  private def parse(item: Item): Versioned[T] = parse(item.data, item.version)
  private def parse(data: Map[String, Any], version: Long): Versioned[T] = Versioned(MapParser.parse(data), version)
}

case class Versioned[T](data: T, version: Long) {
  def map[T2](f: T => T2): Versioned[T2] = {
    val newData = f(data)
    Versioned[T2](newData, version)
  }
}

object Versioned {
  implicit def versioned2t[T <: Parsed[_] : WeakTypeTag](vt: Versioned[T]): T = vt.data
}

object FHCollection {
  type SelectStatement = (String, Any)

  implicit class RichReqSeqField[T : WeakTypeTag : MapDataParser : MapDataProducer](field: FieldRequired[Seq[T]]) {
    def -->(item: T): (String, Any) = {
      field.name -> field.-->(Seq(item))._2.asInstanceOf[Seq[Any]].head
    }
  }

  implicit class RichOptSeqField[T : WeakTypeTag : MapDataParser : MapDataProducer](field: FieldOption[Seq[T]]) {
    def -->(item: T): (String, Any) = {
      field.name -> field.-->(Seq(item))._2.asInstanceOf[Seq[Any]].head
    }
  }

  implicit class RichReqSetField[T : WeakTypeTag : MapDataParser : MapDataProducer](field: FieldRequired[Set[T]]) {
    def -->(item: T): (String, Any) = {
      field.name -> field.-->(Set(item))._2.asInstanceOf[Set[Any]].head
    }
  }

  implicit class RichOptSetField[T : WeakTypeTag : MapDataParser : MapDataProducer](field: FieldOption[Set[T]]) {
    def -->(item: T): (String, Any) = {
      field.name -> field.-->(Set(item))._2.asInstanceOf[Set[Any]].head
    }
  }
}
