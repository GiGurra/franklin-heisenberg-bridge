package se.gigurra.franklinheisenberg

import se.gigurra.franklin._
import se.gigurra.heisenberg._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.reflect.runtime.universe._

case class FHCollection[ObjectType <: Parsed[ObjectType] : WeakTypeTag, SchemaType <: Schema[ObjectType]](franklin: Collection,
                                                                                                          schema: SchemaType,
                                                                                                          defaultValue: () => ObjectType) {
  import FHCollection._

  val tag = weakTypeTag[ObjectType]

  def createUniqueIndex(fGetField: SchemaType => Field[_]): Future[Unit] ={
    val field = fGetField(schema)
    franklin.createUniqueIndex(field.name)
  }

  def create(entity: ObjectType): Future[Unit] = {
    franklin.create(MapProducer.produce(entity))
  }

  case class where_impl(selector: Map[String, Any]) {

    def create(): Future[Unit] = franklin.create(selector)
    def size: Future[Int] = franklin.size(selector)
    def isEmpty: Future[Boolean] = franklin.isEmpty(selector)
    def nonEmpty: Future[Boolean] = franklin.isEmpty(selector)
    def contains: Future[Boolean] = franklin.contains(selector)

    def find: Future[Seq[Versioned[ObjectType]]] = franklin.find(selector).map(parse)

    def update(entity: Versioned[ObjectType], upsert: Boolean = false): Future[Unit] = {
      franklin.update(selector, MapProducer.produce(entity.t), upsert, entity.version)
    }

    def append[T : WeakTypeTag : MapDataParser : MapDataProducer](fGetFieldData: SchemaType => Seq[(FieldRequired[Seq[T]], Seq[T])]): Future[Unit] = {
      val data = fGetFieldData(schema).map { case (k,v) => k.-->(v).asInstanceOf[(String, Seq[Any])]}
      franklin.append(selector, () => MapProducer.produce(defaultValue()), data)
    }

    def append[T : WeakTypeTag : MapDataParser : MapDataProducer: FixErasure1](fGetFieldData: SchemaType => Seq[(FieldOption[Seq[T]], Seq[T])]): Future[Unit] = {
      val data = fGetFieldData(schema).map { case (k,v) => k.-->(v).asInstanceOf[(String, Seq[Any])]}
      franklin.append(selector, () => MapProducer.produce(defaultValue()), data)
    }

    def delete(expectVersion: Long = -1L): Future[Unit] = franklin.deleteItem(selector, expectVersion)
  }

  def where(selector: Map[String, Any]): where_impl = new where_impl(selector)
  def where(statements: SelectStatement*): where_impl = where(statements.toMap)
  def where[T <: Parsed[T] : WeakTypeTag](query: Versioned[T]): where_impl = where(query.t)
  def where(selector: SchemaType => Seq[SelectStatement]): where_impl = where(selector(schema):_*)

  def deleteIndex(index: String)(yeahReally: YeahReally): Future[Unit] = franklin.deleteIndex(index)(yeahReally)

  def wipeItems(): ItemsWiper = franklin.wipeItems()
  def wipeIndices(): IndicesWiper = franklin.wipeIndices()

  private implicit val parser = schema.parser
  private def parse(items: Seq[Item]): Seq[Versioned[ObjectType]] = items.map(parse)
  private def parse(item: Item): Versioned[ObjectType] = parse(item.data, item.version)
  private def parse(data: Map[String, Any], version: Long): Versioned[ObjectType] = Versioned(MapParser.parse(data), version)
}

case class Versioned[T <: Parsed[T] : WeakTypeTag](t: T, version: Long)
object Versioned {
  implicit def versioned2t[T <: Parsed[T] : WeakTypeTag](vt: Versioned[T]): T = vt.t
}

object FHCollection {
  type SelectStatement = (String, Any)
}
