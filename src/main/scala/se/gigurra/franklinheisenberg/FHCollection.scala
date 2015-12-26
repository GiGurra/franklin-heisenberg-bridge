package se.gigurra.franklinheisenberg

import se.gigurra.franklin._
import se.gigurra.heisenberg._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.reflect.runtime.universe._

case class FHCollection[ObjectType <: Parsed[ObjectType] : WeakTypeTag, SchemaType <: Schema[ObjectType]](franklin: Collection,
                                                                                                          schema: SchemaType) {
  import FHCollection._

  val tag = weakTypeTag[ObjectType]

  def createUniqueIndex(fGetField: SchemaType => Field[_]): Future[Unit] ={
    val field = fGetField(schema)
    franklin.createUniqueIndex(field.name)
  }

  def createUniqueIndex(field: Field[_]): Future[Unit] ={
    franklin.createUniqueIndex(field.name)
  }

  def create(entity: ObjectType): Future[Unit] = {
    franklin.create(MapProducer.produce(entity))
  }

  case class where_impl (selector: Map[String, Any]) {

    def create(): Future[Unit] = franklin.create(selector)
    def size: Future[Int] = franklin.size(selector)
    def isEmpty: Future[Boolean] = franklin.isEmpty(selector)
    def nonEmpty: Future[Boolean] = franklin.nonEmpty(selector)
    def contains: Future[Boolean] = franklin.contains(selector)

    def find: Future[Seq[Versioned[ObjectType]]] = franklin.find(selector).map(parse)

    def update(entity: ObjectType, upsert: Boolean = false, expectPrevVersion: Long): Future[Unit] = {
      franklin.update(selector, MapProducer.produce(entity), upsert, expectPrevVersion)
    }

    def append(fGetFieldData: SchemaType => (String, Any), defaultValue: () => ObjectType): Future[Unit] = {
      val data = fGetFieldData(schema).asInstanceOf[(String, Seq[Any])]
      franklin.append(selector, () => MapProducer.produce(defaultValue()), Seq(data))
    }

    def delete(expectVersion: Long = -1L): Future[Unit] = franklin.deleteItem(selector, expectVersion)

    def findOrCreate(ctor: () => ObjectType): Future[Versioned[ObjectType]] = {
      franklin.loadOrCreate(selector, () => MapProducer.produce(ctor())).map(parse)
    }
  }

  def where_raw(selector: Map[String, Any]): where_impl = new where_impl(selector)
  def where_raw(statements: SelectStatement*): where_impl = where_raw(statements.toMap)
  def where[T <: Parsed[T] : WeakTypeTag](query: T): where_impl = where_raw(query)
  def where(selectors: (SchemaType => SelectStatement)*): where_impl = {
    val statements = selectors.map(_.apply(schema))
    where_raw(statements:_*)
  }

  def find_raw(selector: Map[String, Any]): Future[Seq[Versioned[ObjectType]]] = new where_impl(selector).find
  def find_raw(statements: SelectStatement*): Future[Seq[Versioned[ObjectType]]] = find_raw(statements.toMap)
  def find[T <: Parsed[T] : WeakTypeTag](query: T): Future[Seq[Versioned[ObjectType]]] = find_raw(query)
  def find(selectors: (SchemaType => SelectStatement)*): Future[Seq[Versioned[ObjectType]]] = {
    val statements = selectors.map(_.apply(schema))
    find_raw(statements:_*)
  }

  def deleteIndex(index: String)(yeahReally: YeahReally): Future[Unit] = franklin.deleteIndex(index)(yeahReally)
  def deleteIndex(field: Field[_])(yeahReally: YeahReally): Future[Unit] = deleteIndex(field.name)(yeahReally)
  def deleteIndex(fGetField: SchemaType => Field[_])(yeahReally: YeahReally): Future[Unit] = deleteIndex(fGetField(schema))(yeahReally)

  def indices: Future[Seq[String]] = franklin.indices
  def fieldIndices: Future[Seq[Field[Any]]] = indices.map { allIndexNames =>
    val fieldIndexNames = schema.fieldNames.intersect(allIndexNames.toSet).toSeq
    fieldIndexNames.map(schema.field)
  }

  def wipeItems(): ItemsWiper = franklin.wipeItems()
  def wipeIndices(): IndicesWiper = franklin.wipeIndices()


  private implicit val parser = schema.parser
  private def parse(items: Seq[Item]): Seq[Versioned[ObjectType]] = items.map(parse)
  private def parse(item: Item): Versioned[ObjectType] = parse(item.data, item.version)
  private def parse(data: Map[String, Any], version: Long): Versioned[ObjectType] = Versioned(MapParser.parse(data), version)
}

case class Versioned[T <: Parsed[T] : WeakTypeTag](data: T, version: Long)
object Versioned {
  implicit def versioned2t[T <: Parsed[T] : WeakTypeTag](vt: Versioned[T]): T = vt.data
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
}
