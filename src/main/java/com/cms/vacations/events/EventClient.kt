package com.cms.vacations.events

import akka.actor.ActorSystem
import akka.http.javadsl.Http
import akka.http.javadsl.marshallers.jackson.Jackson
import akka.http.javadsl.model.HttpEntity
import akka.http.javadsl.model.HttpRequest
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.unmarshalling.Unmarshaller
import com.cms.vacations.JsonSerializerFactory
import com.cms.vacations.utils.format
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

class EventClient(private val system: ActorSystem, private val http: Http, private val eventsUrl: String) {

    private val eventUnmarshaller: Unmarshaller<HttpEntity, EventResponse> =
        Jackson.unmarshaller(JsonSerializerFactory.jsonSerializer().objectMapper, EventResponse::class.java)

    fun getUserEvents(userId: String, from: LocalDate, to: LocalDate): CompletableFuture<List<Event>> {
        val fromFormatted = from.format()
        val toFormatted = to.format()
        val request = HttpRequest.GET("$eventsUrl/events/$userId/events?from=$fromFormatted&to=$toFormatted")
        return http.singleRequest(request)
            .toCompletableFuture()
            .thenCompose { response ->
                if (response.status() != StatusCodes.OK) {
                    CompletableFuture.completedFuture(emptyList())
                } else {
                    eventUnmarshaller.unmarshal(response.entity(), system)
                        .thenApply { it.events }
                }
            }
    }
}