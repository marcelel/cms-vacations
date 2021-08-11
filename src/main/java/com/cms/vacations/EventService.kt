package com.cms.vacations

import akka.Done
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

interface EventService {

    fun createVacations(user: User, startDate: LocalDate, endDate: LocalDate): CompletableFuture<String>

    fun deleteVacations(eventId: String): CompletableFuture<Done>

    fun canUserTakeVacations(user: User, startDate: LocalDate, endDate: LocalDate): CompletableFuture<Boolean>
}