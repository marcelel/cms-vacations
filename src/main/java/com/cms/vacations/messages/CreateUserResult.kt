package com.cms.vacations.messages

import com.cms.vacations.SerializableMessage

sealed class CreateUserResult : SerializableMessage

data class UserAlreadyExists(val userId: String) : CreateUserResult()

data class UserCreated(val userId: String) : CreateUserResult()
