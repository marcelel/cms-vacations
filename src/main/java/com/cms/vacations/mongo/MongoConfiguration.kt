package com.cms.vacations.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoDatabase
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import java.util.concurrent.TimeUnit

class MongoConfiguration {

    fun create(): MongoDatabase {
        val mongoClientSettings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString("mongodb://localhost:27017"))
            .applyToConnectionPoolSettings { builder ->
                builder
                    .minSize(20)
                    .maxSize(20)
                    .maxConnectionIdleTime(0, TimeUnit.MILLISECONDS)
                    .maxConnectionLifeTime(0, TimeUnit.MILLISECONDS)
                    .maxWaitTime(2500, TimeUnit.MILLISECONDS)
                    .maintenanceFrequency(2000, TimeUnit.MILLISECONDS)
                    .maintenanceInitialDelay(500, TimeUnit.MILLISECONDS)
            }
            .build()
        val client = MongoClients.create(mongoClientSettings)
        val pojoCodecRegistry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
        )
        return client.getDatabase("CmsVacations").withCodecRegistry(pojoCodecRegistry)
    }
}