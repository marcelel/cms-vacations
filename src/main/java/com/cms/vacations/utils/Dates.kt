package com.cms.vacations.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
private val dateFormatter = DateTimeFormatter.ISO_DATE

fun LocalDate.format(): String = this.format(dateFormatter)

fun LocalDateTime.format(): String = this.format(dateTimeFormatter)