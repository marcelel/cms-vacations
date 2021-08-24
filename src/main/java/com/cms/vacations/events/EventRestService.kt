package com.cms.vacations.events

import akka.Done
import com.cms.vacations.EventService
import com.cms.vacations.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.CompletableFuture

class EventRestService(private val eventClient: EventClient) : EventService {

    private val blockingEventTypes = setOf(EventType.DELEGATION)

    override fun createVacations(user: User, startDate: LocalDate, endDate: LocalDate): CompletableFuture<String> {
        val command = CreateEventCommand(
            startDate = LocalDateTime.of(startDate, LocalTime.MIN),
            endDate = LocalDateTime.of(endDate, LocalTime.MAX),
            author = user._id
        )
        return eventClient.createEvent(command)
    }

    override fun deleteVacations(user: User, eventId: String): CompletableFuture<Done> {
        return eventClient.deleteEvent(user._id, eventId)
    }

    override fun canUserTakeVacations(
        user: User,
        startDate: LocalDate,
        endDate: LocalDate
    ): CompletableFuture<Boolean> {
        return eventClient.getUserEvents(user._id, startDate, endDate)
            .thenApply { it.map { it.type } }
            .thenApply { it.none { it in blockingEventTypes } }
    }
}