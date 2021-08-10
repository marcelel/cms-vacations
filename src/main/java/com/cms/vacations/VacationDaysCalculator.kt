package com.cms.vacations

import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

class VacationDaysCalculator(private val publicHolidays: MutableList<LocalDate> = mutableListOf()) {

    private val workingDays = listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)

    fun calculate(startDate: LocalDate, endDate: LocalDate): Int {
        return generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { !endDate.isBefore(it) }
            .filter { it.isWorkingDay() }
            .filter { it !in publicHolidays }
            .count()
    }

    private fun LocalDate.isWorkingDay(): Boolean = dayOfWeek in workingDays
}