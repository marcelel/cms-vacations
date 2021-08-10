package com.cms.vacations.mongo

import com.mongodb.MongoClientSettings
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoDatabase
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider

class MongoConfiguration {

    fun create(): MongoDatabase {
        val client = MongoClients.create("mongodb://localhost:27017")
        val pojoCodecRegistry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
        )
        return client.getDatabase("CmsVacations").withCodecRegistry(pojoCodecRegistry)
    }
}