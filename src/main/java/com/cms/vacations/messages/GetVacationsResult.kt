package com.cms.vacations.messages

import com.cms.vacations.Vacation

sealed class GetVacationsResult

data class GetVacationsUserNotFoundResult(val userId: String) : GetVacationsResult()

data class GetVacationsFoundResult(
    val userId: String,
    val vacations: List<Vacation>
) : GetVacationsResult()
