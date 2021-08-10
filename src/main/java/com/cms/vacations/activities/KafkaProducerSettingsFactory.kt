package com.cms.vacations.activities

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import org.apache.kafka.common.serialization.StringSerializer

class KafkaProducerSettingsFactory(private val system: ActorSystem) {

    fun create(): ProducerSettings<String, String> {
        return ProducerSettings.create(system, StringSerializer(), StringSerializer())
            .withBootstrapServers("localhost:9093")
    }
}