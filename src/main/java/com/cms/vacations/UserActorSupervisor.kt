package com.cms.vacations

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder
import com.cms.vacations.messages.MessageEnvelope

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
            .match(MessageEnvelope::class.java, this::handle)
            .build()
    }


    private fun handle(message: MessageEnvelope) {
        val loyaltyActor = context
            .child(message.to)
            .getOrElse { createActor(message.to) }
        loyaltyActor.forward(message.message, context)
    }

    private fun createActor(id: String): ActorRef {
        return context.actorOf(
            UserActor.create(
                userRepository, vacationRepository, eventService, activityService, vacationDaysCalculator
            ), id
        )
    }
}