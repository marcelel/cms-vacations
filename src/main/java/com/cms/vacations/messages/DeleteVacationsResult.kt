package com.cms.vacations.messages

sealed class DeleteVacationsResult

data class DeleteVacationsUserNotFoundResult(val userId: String) : DeleteVacationsResult()

data class DeleteVacationsVacationsNotFoundResult(val vacationsId: String) : DeleteVacationsResult()

data class DeleteVacationsDeletedResult(val vacationsId: String) : DeleteVacationsResult()