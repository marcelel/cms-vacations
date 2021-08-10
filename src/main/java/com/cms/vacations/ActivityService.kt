package com.cms.vacations

import akka.Done
import java.util.concurrent.CompletableFuture

interface ActivityService {

    fun publish(activity: Activity): CompletableFuture<Done>
}