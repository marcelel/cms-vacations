package com.cms.vacations.messages

import com.cms.vacations.User
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import java.time.LocalDate

sealed class VacationMessage

object GetUserQuery : VacationMessage()

data class CreateUserCommand(val vacationDaysLeft: Int) : VacationMessage()

data class GetUserQueryResult(val user: User?) : VacationMessage()

data class CreateVacationsCommand(
    @JsonDeserialize(using = LocalDateDeserializer::class)
    val startDate: LocalDate,
    @JsonDeserialize(using = LocalDateDeserializer::class)
    val endDate: LocalDate
) : VacationMessage()

data class DeleteVacationsCommand(val vacationsId: String) : VacationMessage()