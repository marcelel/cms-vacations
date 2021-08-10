package com.cms.vacations

import java.time.LocalDate
import java.util.concurrent.CompletableFuture

interface EventService {

    fun canUserTakeVacations(user: User, startDate: LocalDate, endDate: LocalDate): CompletableFuture<Boolean>
}