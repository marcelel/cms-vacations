package com.cms.vacations.activities

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.javadsl.Producer
import akka.stream.javadsl.Source
import com.cms.vacations.Activity
import com.cms.vacations.ActivityService
import com.cms.vacations.JsonSerializerFactory
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.concurrent.CompletableFuture

class ActivitiesKafkaService(
    private val system: ActorSystem,
    private val producerSettings: ProducerSettings<String, String>,
    private val topicName: String
) : ActivityService {

    private val jsonSerializer = JsonSerializerFactory.jsonSerializer()

    override fun publish(activity: Activity): CompletableFuture<Done> {
        return Source.single(jsonSerializer.objectMapper.writeValueAsString(activity))
            .map { ProducerRecord<String, String>(topicName, it) }
            .runWith(Producer.plainSink(producerSettings), system)
            .toCompletableFuture()
    }
}