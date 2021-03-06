package com.github.gigurra.franklinheisenberg

import java.io.Closeable

import com.github.gigurra.heisenberg.{Schema, Parsed}
import scala.reflect.runtime.universe._

/**
  * Created by johan on 2015-12-23.
  */
trait FHStore extends Closeable {

  def getOrCreate[T <: Parsed[S] : WeakTypeTag, S <: Schema[T]](collectionName: String,
                                                                schema: S with Schema[T]): FHCollection[T, S]

  def close(): Unit

}
