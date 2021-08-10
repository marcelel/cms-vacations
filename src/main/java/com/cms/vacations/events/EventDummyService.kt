package com.cms.vacations.events

import com.cms.vacations.EventService
import com.cms.vacations.User
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

class EventDummyService : EventService {

    override fun canUserTakeVacations(
        user: User,
        startDate: LocalDate,
        endDate: LocalDate
    ): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(true)
    }
}