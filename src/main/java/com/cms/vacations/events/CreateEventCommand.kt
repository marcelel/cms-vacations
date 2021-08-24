package com.cms.vacations.events

import java.time.LocalDateTime

data class CreateEventCommand(
    val title: String = "Vacations",
    val description: String = "Vacations",
    val type: EventType = EventType.VACATIONS,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val author: String
)