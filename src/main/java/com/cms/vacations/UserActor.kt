package com.cms.vacations

import akka.Done
import akka.actor.AbstractActorWithStash
import akka.actor.Props
import akka.pattern.Patterns.pipe
import com.cms.vacations.utils.format
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture

class UserActor private constructor(
    private val userRepository: UserRepository,
    private val vacationRepository: VacationRepository,
    private val eventService: EventService,
    private val activityService: ActivityService,
    private val vacationDaysCalculator: VacationDaysCalculator
) : AbstractActorWithStash() {

    private val userId = self.path().name()
    private var user: User? = null
    private var vacations: MutableList<Vacation> = mutableListOf()

    companion object {

        @JvmStatic
        fun create(
            userRepository: UserRepository,
            vacationRepository: VacationRepository,
            eventService: EventService,
            activityService: ActivityService,
            vacationDaysCalculator: VacationDaysCalculator
        ): Props {
            return Props.create(UserActor::class.java) {
                UserActor(userRepository, vacationRepository, eventService, activityService, vacationDaysCalculator)
            }
        }
    }

    override fun preStart() {
        super.preStart()
        val initialized = userRepository.findOne(userId)
            .thenCompose { user ->
                if (user == null) {
                    CompletableFuture.completedFuture(Initialized(null, null))
                } else {
                    vacationRepository.findByUserId(userId).thenApply { Initialized(user, it) }
                }
            }
        pipe(initialized, context.dispatcher).to(self)
    }

    override fun createReceive(): Receive {
        return initializing()
    }

    private fun initializing(): Receive {
        return receiveBuilder()
            .match(
                Initialized::class.java
            ) {
                user = it.user
                vacations = it.vacations?.toMutableList() ?: mutableListOf()
                context.become(running())
                unstashAll()
            }
            .matchAny { stash() }
            .build()
    }

    private fun running(): Receive {
        return receiveBuilder()
            .match(GetUserQuery::class.java, this::handle)
            .match(CreateUserCommand::class.java, this::handle)
            .match(CreateVacationsCommand::class.java, this::handle)
            .build()
    }

    private fun handle(ignored: GetUserQuery) {
        sender.tell(GetUserQueryResult(user), self)
    }

    private fun handle(command: CreateUserCommand) {
        if (user != null) {
            sender.tell(UserAlreadyExists(userId), self)
            return
        }

        user = User(userId, command.vacationDaysLeft, 0)
        val result = userRepository.save(user!!)
        pipe(result, context.dispatcher).to(sender) //todo: validate that Done works
    }

    private fun handle(command: CreateVacationsCommand) {
        if (user == null) {
            sender.tell(UserNotFound(userId), self)
            return
        }

        val vacationDays = vacationDaysCalculator.calculate(command.startDate, command.endDate)
        if (user!!.vacationDaysLeft < vacationDays) {
            val reason = "Insufficient days left: ${user!!.vacationDaysLeft}, required: $vacationDays"
            sender.tell(VacationRejected(userId, reason), self)
            return
        }

        val result = eventService.canUserTakeVacations(user!!, command.startDate, command.endDate)
            .thenCompose {
                if (!it) {
                    val reason = "Important events in specified period"
                    CompletableFuture.completedFuture<VacationMessage>(VacationRejected(userId, reason))
                } else {
                    addVacation(command).thenCompose { updateUser(vacationDays) }
                        .thenCompose { publishActivity(command) }
                        .thenApply<VacationMessage> {
                            VacationSubmitted(userId, command.startDate, command.endDate, vacationDays)
                        }
                }
            }

        pipe(result, context.dispatcher).to(sender) //todo: validate that Done works
    }

    private fun handle(command: DeleteVacationsCommand) {
        if (user == null) {
            sender.tell(UserNotFound(userId), self)
            return
        }

        val vacation = vacations.find { it._id == command.vacationsId }
        if (vacation == null) {
            sender.tell(VacationsNotFound(command.vacationsId), self)
            return
        }

        vacations.remove(vacation)
        val result = vacationRepository.delete(vacation)
        pipe(result, context.dispatcher).to(sender) //todo: validate that Done works
    }

    private fun addVacation(command: CreateVacationsCommand): CompletableFuture<Done> {
        val vacation = Vacation(
            _id = UUID.randomUUID().toString(),
            userId = userId,
            start = command.startDate,
            end = command.endDate,
            vacationDays = user!!.vacationDaysTaken
        )
        vacations.add(vacation)
        return vacationRepository.save(vacation)
    }

    private fun updateUser(vacationDays: Int): CompletableFuture<Done> {
        user = user!!.copy(
            vacationDaysLeft = user!!.vacationDaysLeft - vacationDays,
            vacationDaysTaken = user!!.vacationDaysTaken + vacationDays
        )
        return userRepository.update(user!!)
    }

    private fun publishActivity(command: CreateVacationsCommand): CompletableFuture<Done> {
        val activity = Activity(
            userId,
            LocalDateTime.now(),
            "Vacations from ${command.startDate.format()} to ${command.endDate.format()}"
        )
        return activityService.publish(activity)
    }

    private data class Initialized(val user: User?, val vacations: List<Vacation>?)
}