package se.gigurra.franklinheisenberg

import se.gigurra.franklin.mongoimpl.{DefaultBsonCodec, BsonCodec}
import se.gigurra.franklin.{Franklin, Store}
import se.gigurra.heisenberg.{Parsed, Schema}

import scala.reflect.runtime.universe._

/**
  * Created by kjolh on 12/25/2015.
  */
object FranklinHeisenberg {

  private def buildStore(franklinStore: Store): FHStore = new FHStore {

    override def getOrCreate[T <: Parsed[S] : WeakTypeTag, S <: Schema[T]](collectionName: String,
                                                                           schema: S with Schema[T]): FHCollection[T, S] = {
      val franklinCollection = franklinStore.getOrCreate(collectionName)
      new FHCollection[T, S](franklinCollection, schema)
    }

    override def close(): Unit = franklinStore.close()

  }

  def loadMongo(database: String = "local", nodes: Seq[String] = Seq("127.0.0.1:27017"), codec: BsonCodec = DefaultBsonCodec): FHStore = {
    buildStore(Franklin.loadMongo(database, nodes, codec))
  }

  def loadInMemory(): FHStore = {
    buildStore(Franklin.loadInMemory())
  }

}
