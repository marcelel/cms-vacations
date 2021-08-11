package com.cms.vacations.messages

sealed class AddVacationResult

data class AddVacationUserNotFoundResult(val userId: String) : AddVacationResult()

data class AddVacationRejectedResult(val userId: String, val reason: String) : AddVacationResult()

data class AddVacationSubmittedResult(
    val userId: String,
    val vacationsId: String
) : AddVacationResult()
