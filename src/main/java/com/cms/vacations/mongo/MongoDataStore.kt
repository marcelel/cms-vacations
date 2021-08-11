package com.cms.vacations.mongo

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.mongodb.DocumentReplace
import akka.stream.alpakka.mongodb.javadsl.MongoSink
import akka.stream.alpakka.mongodb.javadsl.MongoSource
import akka.stream.javadsl.Sink
import akka.stream.javadsl.Source
import com.cms.vacations.JsonSerializerFactory
import com.mongodb.client.model.Filters
import com.mongodb.reactivestreams.client.MongoDatabase
import org.bson.Document
import java.util.concurrent.CompletableFuture

class MongoDataStore<T>(
    private val system: ActorSystem,
    private val collectionName: String,
    private val mongoDatabase: MongoDatabase,
    private val clazz: Class<T>
) {

    private val ID_FIELD = "_id"
    private val objectMapper = JsonSerializerFactory.jsonSerializer().objectMapper
    private val listClass = objectMapper.typeFactory.constructCollectionType(List::class.java, clazz)

    fun find(id: String): CompletableFuture<T?> {
        val collection = mongoDatabase.getCollection(collectionName)
        val publisher = collection.find(Document().append(ID_FIELD, id)).first()
        val source = MongoSource.create(publisher)
        return source.runWith(Sink.seq(), system)
            .toCompletableFuture()
            .thenApply { if (it.size > 0) it.first() else null }
            .thenApply { if (it != null) objectMapper.convertValue(it, clazz) else null }
    }

    fun findBy(fields: Map<String, String>): CompletableFuture<List<T>> {
        val collection = mongoDatabase.getCollection(collectionName)
        val document = Document()
        fields.forEach { document.append(it.key, it.value) }
        val publisher = collection.find(document)
        val source = MongoSource.create(publisher)
        return source.runWith(Sink.seq(), system)
            .toCompletableFuture()
            .thenApply<List<T>> { if (it != null) objectMapper.convertValue(it, listClass) else emptyList() }
    }

    fun save(entity: T): CompletableFuture<Done> {
        val collection = mongoDatabase.getCollection(collectionName)
        val json = objectMapper.writeValueAsString(entity)
        val document = Document.parse(json)
        return Source.single(document).runWith(MongoSink.insertOne(collection), system)
            .toCompletableFuture()
    }

    fun update(id: String, entity: T): CompletableFuture<Done> {
        val collection = mongoDatabase.getCollection(collectionName)
        val json = objectMapper.writeValueAsString(entity)
        val document = Document.parse(json)
        val documentReplace = DocumentReplace.create(Filters.eq("_id", id), document)
        return Source.single(documentReplace).runWith(MongoSink.replaceOne(collection), system)
            .toCompletableFuture()
    }

    fun delete(id: String): CompletableFuture<Done> {
        val collection = mongoDatabase.getCollection(collectionName)
        return Source.single(Filters.eq("_id", id)).runWith(MongoSink.deleteOne(collection), system)
            .toCompletableFuture()
    }
}