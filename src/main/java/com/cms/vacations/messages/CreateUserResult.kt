package com.cms.vacations.messages

sealed class CreateUserResult

data class UserAlreadyExists(val userId: String) : CreateUserResult()

data class UserCreated(val userId: String) : CreateUserResult()
