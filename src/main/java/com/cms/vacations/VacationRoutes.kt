package com.cms.vacations

import akka.actor.ActorRef
import akka.http.javadsl.marshallers.jackson.Jackson
import akka.http.javadsl.model.ContentTypes
import akka.http.javadsl.model.HttpEntity
import akka.http.javadsl.model.HttpHeader
import akka.http.javadsl.model.HttpResponse
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.PathMatchers.segment
import akka.http.javadsl.server.Route
import akka.http.javadsl.unmarshalling.Unmarshaller
import akka.pattern.Patterns.ask
import com.cms.vacations.messages.AddVacationRejectedResult
import com.cms.vacations.messages.AddVacationResult
import com.cms.vacations.messages.AddVacationSubmittedResult
import com.cms.vacations.messages.AddVacationUserNotFoundResult
import com.cms.vacations.messages.CreateUserCommand
import com.cms.vacations.messages.CreateVacationsCommand
import com.cms.vacations.messages.DeleteVacationsCommand
import com.cms.vacations.messages.DeleteVacationsDeletedResult
import com.cms.vacations.messages.DeleteVacationsResult
import com.cms.vacations.messages.DeleteVacationsUserNotFoundResult
import com.cms.vacations.messages.DeleteVacationsVacationsNotFoundResult
import com.cms.vacations.messages.MessageEnvelope
import com.cms.vacations.messages.UserCreated
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.time.Duration

class VacationRoutes(private val userActorSupervisor: ActorRef) : AllDirectives() {

    private val timeout = Duration.ofSeconds(5)
    private val objectMapper = getObjectMapper()
    private val createVacationCommandUnmarshaller: Unmarshaller<HttpEntity, CreateVacationsCommand> =
        Jackson.unmarshaller(JsonSerializerFactory.jsonSerializer().objectMapper, CreateVacationsCommand::class.java)
    private val createUserCommandUnmarshaller: Unmarshaller<HttpEntity, CreateUserCommand> =
        Jackson.unmarshaller(JsonSerializerFactory.jsonSerializer().objectMapper, CreateUserCommand::class.java)

    fun createRoutes(): Route {
        return pathPrefix(
            "vacations"
        ) {
            concat(
                pathPrefix("users") {
                    concat(
                        pathEnd {
                            post { createUser() }
                        },
                        pathPrefix(
                            segment()
                        ) { userId ->
                            concat(
                                pathPrefix("vacations") {
                                    concat(
                                        pathEnd {
                                            post { addVacations(userId) }
                                        },
                                        pathPrefix(
                                            segment()
                                        ) { vacationId ->
                                            pathEnd {
                                                delete { deleteVacations(userId, vacationId) }
                                            }
                                        }
                                    )
                                })
                        }
                    )
                }
            )
        }
    }

    private fun createUser(): Route {
        return entity(createUserCommandUnmarshaller) { entity ->
            val result = ask(userActorSupervisor, MessageEnvelope(entity), timeout)
                .thenApply { it as UserCreated }
                .thenApply { created(it.userId) }
                .thenApply { complete(it) }
            onComplete(result) { it.get() }
        }
    }

    private fun addVacations(username: String): Route {
        return entity(createVacationCommandUnmarshaller) { entity ->
            val result = ask(userActorSupervisor, MessageEnvelope(entity, username), timeout)
                .thenApply { it as AddVacationResult }
                .thenApply {
                    when (it) {
                        is AddVacationUserNotFoundResult -> badRequest("User ${it.userId} not found")
                        is AddVacationRejectedResult -> badRequest(it.reason)
                        is AddVacationSubmittedResult -> created(it.vacationsId)
                    }
                }.thenApply { complete(it) }
            onComplete(result) { it.get() }
        }
    }

    private fun deleteVacations(userId: String, vacationsId: String): Route {
        val result = ask(
            userActorSupervisor,
            MessageEnvelope(DeleteVacationsCommand(vacationsId), userId),
            timeout
        )
            .thenApply { it as DeleteVacationsResult }
            .thenApply {
                when (it) {
                    is DeleteVacationsUserNotFoundResult -> badRequest("User ${it.userId} not found")
                    is DeleteVacationsVacationsNotFoundResult -> badRequest("Vacation ${it.vacationsId} not found")
                    is DeleteVacationsDeletedResult -> HttpResponse.create()
                        .addHeader(HttpHeader.parse("Content-Type", "application/json"))
                }
            }.thenApply { complete(it) }
        return onComplete(result) { it.get() }
    }

    private fun badRequest(message: String): HttpResponse {
        return HttpResponse.create()
            .withStatus(StatusCodes.BAD_REQUEST)
            .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
            .addHeader(HttpHeader.parse("Content-Type", "application/json"))
            .withEntity(ContentTypes.APPLICATION_JSON, message)
    }

    private fun created(message: String): HttpResponse {
        return HttpResponse.create()
            .withStatus(StatusCodes.CREATED)
            .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
            .addHeader(HttpHeader.parse("Content-Type", "application/json"))
            .withEntity(ContentTypes.APPLICATION_JSON, message)
    }

    private fun getObjectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        return mapper
    }
}