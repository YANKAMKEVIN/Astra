package com.kevin.astra.core.notification

import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

actual fun createNotificationService(): NotificationService = IosNotificationService()

class IosNotificationService : NotificationService {
    override fun showNotification(title: String, message: String, targetDestination: String?) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
            // For iOS navigation, we would usually handle this in the AppDelegate
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "astra_ai_${platform.Foundation.NSUUID().UUIDString()}",
            content = content,
            trigger = null
        )

        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { error ->
            // Handle error
        }
    }
}
