package se.gigurra.franklinheisenberg

import se.gigurra.franklin.{Franklin, Store}
import se.gigurra.heisenberg.{Parsed, Schema}

import scala.reflect.runtime.universe._

/**
  * Created by kjolh on 12/25/2015.
  */
object FranklinHeisenberg {

  private def buildStore(franklinStore: Store): FHStore = new FHStore {

    override def getOrCreate[ObjectType <: Parsed[ObjectType] : WeakTypeTag, SchemaType <: Schema[ObjectType]](collectionName: String,
                                                                                                               schema: SchemaType): FHCollection[ObjectType, SchemaType] = {
      val franklinCollection = franklinStore.getOrCreate(collectionName)
      new FHCollection(franklinCollection, schema)
    }

    override def close(): Unit = franklinStore.close()

  }

  def loadMongo(database: String = "local", nodes: Seq[String] = Seq("127.0.0.1:27017")): FHStore = {
    buildStore(Franklin.loadMongo(database, nodes))
  }

  def loadInMemory(): FHStore = {
    buildStore(Franklin.loadInMemory())
  }

}
