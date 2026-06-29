package com.kevin.astra.core.notification

import com.kevin.astra.core.navigation.AstraDestination

interface NotificationService {
    fun showNotification(
        title: String,
        message: String,
        targetDestination: AstraDestination? = null
    )
}

object NotificationKeys {
    const val TARGET_DESTINATION = "EXTRA_TARGET_DESTINATION"
}

expect fun createNotificationService(): NotificationService
