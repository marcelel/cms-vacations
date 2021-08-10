package com.cms.vacations

import akka.Done
import com.cms.vacations.mongo.MongoDataStore
import java.util.concurrent.CompletableFuture

interface UserRepository {

    fun save(user: User): CompletableFuture<Done>

    fun update(user: User): CompletableFuture<Done>

    fun findOne(userId: String): CompletableFuture<User?>
}

class UserMongoRepository(private val mongoDataStore: MongoDataStore<User>) : UserRepository {

    override fun save(user: User): CompletableFuture<Done> {
        return mongoDataStore.save(user)
    }

    override fun update(user: User): CompletableFuture<Done> {
        return mongoDataStore.update(user._id, user)
    }

    override fun findOne(userId: String): CompletableFuture<User?> {
        return mongoDataStore.find(userId)
    }
}