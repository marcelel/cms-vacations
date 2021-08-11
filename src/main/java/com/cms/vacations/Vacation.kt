package com.cms.vacations

import java.time.LocalDate

data class Vacation(
    val _id: String,
    val userId: String,
    val eventId: String,
    val start: LocalDate,
    val end: LocalDate,
    val vacationDays: Int
)