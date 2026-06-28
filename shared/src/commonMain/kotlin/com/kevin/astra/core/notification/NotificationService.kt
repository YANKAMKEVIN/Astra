package com.kevin.astra.core.notification

interface NotificationService {
    fun showNotification(
        title: String,
        message: String,
        targetDestination: String? = null
    )
}

expect fun createNotificationService(): NotificationService
