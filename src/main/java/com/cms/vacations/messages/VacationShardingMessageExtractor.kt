package com.cms.vacations.messages

import akka.cluster.sharding.ShardRegion
import kotlin.math.abs

class VacationShardingMessageExtractor(private val maxShards: Int) : ShardRegion.MessageExtractor {

    override fun entityId(message: Any?): String? {
        return if (message is MessageEnvelope) message.to else null
    }

    override fun entityMessage(message: Any?): Any? {
        return if (message is MessageEnvelope) message.message else null
    }

    override fun shardId(message: Any?): String? {
        return if (message is MessageEnvelope) abs(message.to.hashCode() % maxShards).toString() else null
    }
}