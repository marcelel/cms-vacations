package com.cms.vacations.events

import akka.Done
import com.cms.vacations.EventService
import com.cms.vacations.User
import java.time.LocalDate
import java.util.*
import java.util.concurrent.CompletableFuture

class EventDummyService : EventService {

    override fun createVacations(user: User, startDate: LocalDate, endDate: LocalDate): CompletableFuture<String> {
        return CompletableFuture.completedFuture(UUID.randomUUID().toString())
    }

    override fun deleteVacations(eventId: String): CompletableFuture<Done> {
        return CompletableFuture.completedFuture(Done.done())
    }

    override fun canUserTakeVacations(
        user: User,
        startDate: LocalDate,
        endDate: LocalDate
    ): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(true)
    }
}