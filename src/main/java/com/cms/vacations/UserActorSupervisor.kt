package com.cms.vacations

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder
import com.cms.vacations.messages.VacationMessage
import java.util.*

class UserActorSupervisor(
    private val userRepository: UserRepository,
    private val vacationRepository: VacationRepository,
    private val eventService: EventService,
    private val activityService: ActivityService,
    private val vacationDaysCalculator: VacationDaysCalculator
) : AbstractActor() {

    companion object {

        @JvmStatic
        fun create(
            userRepository: UserRepository,
            vacationRepository: VacationRepository,
            eventService: EventService,
            activityService: ActivityService,
            vacationDaysCalculator: VacationDaysCalculator
        ): Props {
            return Props.create(UserActorSupervisor::class.java) {
                UserActorSupervisor(
                    userRepository, vacationRepository, eventService, activityService, vacationDaysCalculator
                )
            }
        }
    }

    override fun createReceive(): Receive {
        return ReceiveBuilder.create()
            .match(Message::class.java, this::handle)
            .build()
    }

    data class Message(val to: String = UUID.randomUUID().toString(), val vacationMessage: VacationMessage)

    private fun handle(message: Message) {
        val loyaltyActor = context
            .child(message.to)
            .getOrElse { createActor(message.to) }
        loyaltyActor.forward(message.vacationMessage, context)
    }

    private fun createActor(id: String): ActorRef {
        return context.actorOf(
            UserActor.create(
                userRepository, vacationRepository, eventService, activityService, vacationDaysCalculator
            ), id
        )
    }
}