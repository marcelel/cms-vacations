package com.cms.vacations

import akka.Done
import akka.actor.AbstractActorWithStash
import akka.actor.Props
import akka.pattern.Patterns.pipe
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
import com.cms.vacations.messages.GetUserQuery
import com.cms.vacations.messages.GetUserQueryResult
import com.cms.vacations.messages.UserAlreadyExists
import com.cms.vacations.messages.UserCreated
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
//        val initialized = CompletableFuture.completedFuture(Initialized(null, null))
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
            .match(DeleteVacationsCommand::class.java, this::handle)
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
//        val result = CompletableFuture.completedFuture(UserCreated(userId))
        val result = userRepository.save(user!!)
            .thenApply { UserCreated(userId) }
        pipe(result, context.dispatcher).to(sender)
    }

    private fun handle(command: CreateVacationsCommand) {
        if (user == null) {
            sender.tell(AddVacationUserNotFoundResult(userId), self)
            return
        }

        val vacationDays = vacationDaysCalculator.calculate(command.startDate, command.endDate)
        if (user!!.vacationDaysLeft < vacationDays) {
            val reason = "Insufficient days left: ${user!!.vacationDaysLeft}, required: $vacationDays"
            sender.tell(AddVacationRejectedResult(userId, reason), self)
            return
        }

        val result = eventService.canUserTakeVacations(user!!, command.startDate, command.endDate)
            .thenCompose { userCanTakeVacations ->
                if (!userCanTakeVacations) {
                    val reason = "Important events in specified period"
                    CompletableFuture.completedFuture<AddVacationResult>(AddVacationRejectedResult(userId, reason))
                } else {
                    eventService.createVacations(user!!, command.startDate, command.endDate)
                        .thenCompose { addVacation(command, it, vacationDays) }
                        .thenCompose { vacation -> updateUser(vacationDays).thenApply { vacation } }
                        .thenCompose { vacation -> publishActivity("Vacations from ${command.startDate.format()} to ${command.endDate.format()}").thenApply { vacation } }
                        .thenApply<AddVacationResult> { vacation -> AddVacationSubmittedResult(userId, vacation._id) }
                }
            }

        pipe(result, context.dispatcher).to(sender)
    }

    private fun handle(command: DeleteVacationsCommand) {
        if (user == null) {
            sender.tell(DeleteVacationsUserNotFoundResult(userId), self)
            return
        }

        val vacation = vacations.find { it._id == command.vacationsId }
        if (vacation == null) {
            sender.tell(DeleteVacationsVacationsNotFoundResult(command.vacationsId), self)
            return
        }

        vacations.remove(vacation)
        user = user!!.copy(
            vacationDaysLeft = user!!.vacationDaysLeft + vacation.vacationDays,
            vacationDaysTaken = user!!.vacationDaysTaken - vacation.vacationDays
        )

        val result = eventService.deleteVacations(vacation.eventId)
            .thenCompose { vacationRepository.delete(vacation) }
            .thenCompose { userRepository.update(user!!) }
            .thenCompose {
                val activityMessage = "Vacations from ${vacation.start.format()} to ${vacation.end.format()}"
                publishActivity(activityMessage).thenApply { vacation }
            }
            .thenApply<DeleteVacationsResult> { DeleteVacationsDeletedResult(vacation._id) }

        pipe(result, context.dispatcher).to(sender)
    }

    private fun addVacation(
        command: CreateVacationsCommand,
        eventId: String,
        vacationDays: Int
    ): CompletableFuture<Vacation> {
        val vacation = Vacation(
            _id = UUID.randomUUID().toString(),
            userId = userId,
            eventId = eventId,
            start = command.startDate,
            end = command.endDate,
            vacationDays = vacationDays
        )
        vacations.add(vacation)
        return vacationRepository.save(vacation).thenApply { vacation }
    }

    private fun updateUser(vacationDays: Int): CompletableFuture<Done> {
        user = user!!.copy(
            vacationDaysLeft = user!!.vacationDaysLeft - vacationDays,
            vacationDaysTaken = user!!.vacationDaysTaken + vacationDays
        )
        return userRepository.update(user!!)
    }

    private fun publishActivity(message: String): CompletableFuture<Done> {
        val activity = Activity(
            userId,
            LocalDateTime.now(),
            message
        )
        return activityService.publish(activity)
    }

    private data class Initialized(val user: User?, val vacations: List<Vacation>?)
}