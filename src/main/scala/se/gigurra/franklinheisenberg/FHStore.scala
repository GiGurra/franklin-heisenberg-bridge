package se.gigurra.franklinheisenberg

import java.io.Closeable

import se.gigurra.heisenberg.{Schema, Parsed}
import scala.reflect.runtime.universe._

/**
  * Created by johan on 2015-12-23.
  */
trait FHStore extends Closeable {

  def getOrCreate[ObjectType <: Parsed[ObjectType] : WeakTypeTag, SchemaType <: Schema[ObjectType]](collectionName: String,
                                                                                                    schema: SchemaType): FHCollection[ObjectType, SchemaType]

  def close(): Unit

}
