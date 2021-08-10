package com.cms.vacations.events

import java.time.LocalDateTime

data class Event(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val type: EventType
)
