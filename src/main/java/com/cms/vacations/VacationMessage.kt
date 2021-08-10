package com.cms.vacations

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import java.time.LocalDate

sealed class VacationMessage

object GetUserQuery : VacationMessage()

data class CreateUserCommand(val vacationDaysLeft: Int) : VacationMessage()

data class UserAlreadyExists(val userId: String) : VacationMessage()

data class UserNotFound(val userId: String) : VacationMessage()

data class VacationRejected(val userId: String, val reason: String) : VacationMessage()

data class VacationSubmitted(
    val userId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val vacationDays: Int
) : VacationMessage()

data class GetUserQueryResult(val user: User?) : VacationMessage()

data class CreateVacationsCommand(
    @JsonDeserialize(using = LocalDateDeserializer::class)
    val startDate: LocalDate,
    @JsonDeserialize(using = LocalDateDeserializer::class)
    val endDate: LocalDate
) : VacationMessage()

data class DeleteVacationsCommand(val userId: String, val vacationsId: String) : VacationMessage()

data class VacationsNotFound(val vacationsId: String) : VacationMessage()