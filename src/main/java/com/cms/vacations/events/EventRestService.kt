package com.cms.vacations.events

import com.cms.vacations.EventService
import com.cms.vacations.User
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

class EventRestService(private val eventClient: EventClient) : EventService {

    private val blockingEventTypes = setOf(EventType.DELEGATION)

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