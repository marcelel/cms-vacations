package com.cms.vacations.messages

import com.cms.vacations.SerializableMessage

sealed class DeleteVacationsResult : SerializableMessage

data class DeleteVacationsUserNotFoundResult(val userId: String) : DeleteVacationsResult()

data class DeleteVacationsVacationsNotFoundResult(val vacationsId: String) : DeleteVacationsResult()

data class DeleteVacationsDeletedResult(val vacationsId: String) : DeleteVacationsResult()