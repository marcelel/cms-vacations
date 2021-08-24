package com.cms.vacations.events

import akka.Done
import akka.actor.ActorSystem
import akka.http.javadsl.Http
import akka.http.javadsl.marshallers.jackson.Jackson
import akka.http.javadsl.model.ContentTypes
import akka.http.javadsl.model.HttpEntity
import akka.http.javadsl.model.HttpRequest
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.unmarshalling.Unmarshaller
import com.cms.vacations.JsonSerializerFactory
import com.cms.vacations.utils.format
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.CompletableFuture

class EventClient(private val system: ActorSystem, private val http: Http, private val eventsUrl: String) {

    private val objectMapper = JsonSerializerFactory.jsonSerializer().doNotFailOnUnknownProperties().objectMapper
    private val eventUnmarshaller: Unmarshaller<HttpEntity, EventResponse> =
        Jackson.unmarshaller(objectMapper, EventResponse::class.java)

    fun createEvent(command: CreateEventCommand): CompletableFuture<String> {
        val json = objectMapper.writeValueAsString(command)
        val request = HttpRequest.POST("$eventsUrl/events/users/${command.author}/events")
            .withEntity(ContentTypes.APPLICATION_JSON, json)
        return http.singleRequest(request)
            .toCompletableFuture()
            .thenCompose { response ->
                if (response.status() == StatusCodes.CREATED) {
                    Unmarshaller.entityToString().unmarshal(response.entity(), system)
                } else {
                    throw IllegalStateException("Unexpected events response code ${response.status()}")
                }
            }
    }

    fun deleteEvent(userId: String, eventId: String): CompletableFuture<Done> {
        val request = HttpRequest.DELETE("$eventsUrl/events/users/${userId}/events/$eventId")
        return http.singleRequest(request)
            .toCompletableFuture()
            .thenApply { response ->
                if (response.status() == StatusCodes.NO_CONTENT) {
                    Done.done()
                } else {
                    throw IllegalStateException("Unexpected events response code ${response.status()}")
                }
            }
    }

    fun getUserEvents(userId: String, from: LocalDate, to: LocalDate): CompletableFuture<List<Event>> {
        val fromFormatted = LocalDateTime.of(from, LocalTime.MIN).format()
        val toFormatted = LocalDateTime.of(to, LocalTime.MAX).format()
        val request = HttpRequest.GET(
            "$eventsUrl/events/users/$userId/events?startDate=$fromFormatted&endDate=$toFormatted"
        )
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