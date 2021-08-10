package com.cms.vacations

import akka.Done
import com.cms.vacations.mongo.MongoDataStore
import java.util.concurrent.CompletableFuture

interface VacationRepository {

    fun save(vacation: Vacation): CompletableFuture<Done>

    fun delete(vacation: Vacation): CompletableFuture<Done>

    fun findByUserId(userId: String): CompletableFuture<List<Vacation>>
}

class VacationMongoRepository(private val mongoDataStore: MongoDataStore<Vacation>) : VacationRepository {

    override fun save(vacation: Vacation): CompletableFuture<Done> {
        return mongoDataStore.save(vacation)
    }

    override fun delete(vacation: Vacation): CompletableFuture<Done> {
        return mongoDataStore.delete(vacation._id)
    }

    override fun findByUserId(userId: String): CompletableFuture<List<Vacation>> {
        return mongoDataStore.findBy(mapOf("userId" to userId))
    }
}