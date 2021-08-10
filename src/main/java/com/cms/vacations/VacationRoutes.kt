package com.cms.vacations

import akka.actor.ActorRef
import akka.http.javadsl.marshallers.jackson.Jackson
import akka.http.javadsl.model.HttpEntity
import akka.http.javadsl.model.HttpHeader
import akka.http.javadsl.model.HttpResponse
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.PathMatchers
import akka.http.javadsl.server.Route
import akka.http.javadsl.unmarshalling.Unmarshaller
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

class VacationRoutes(private val userActorSupervisor: ActorRef) : AllDirectives() {

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
                            PathMatchers.segment()
                        ) {
                            pathEnd {
                                post { addVacations(it) }
                            }
                        }
                    )
                }
            )
        }
    }

    private fun createUser(): Route {
        return entity(createUserCommandUnmarshaller) {
            userActorSupervisor.tell(UserActorSupervisor.Message(vacationMessage = it), ActorRef.noSender())
            complete(
                HttpResponse.create()
                    .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
                    .withStatus(StatusCodes.CREATED)
            )
        }
    }

    private fun addVacations(username: String): Route {
        return entity(createVacationCommandUnmarshaller) {
            userActorSupervisor.tell(UserActorSupervisor.Message(username, it), ActorRef.noSender())
            complete(
                HttpResponse.create()
                    .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
                    .withStatus(StatusCodes.CREATED)
            )
        }
    }

    private fun getAllVacations(username: String): Route {
        return try {
            complete(HttpResponse.create()
                    .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
                    .withEntity("[]"))
        } catch (e: JsonProcessingException) {
            complete("[]")
        }
    }

    private fun getDaysOffLeft(username: String): Route {
        return complete(HttpResponse.create()
                .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
                .withEntity("20")
        )
    }

    private fun getObjectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        return mapper
    }
}