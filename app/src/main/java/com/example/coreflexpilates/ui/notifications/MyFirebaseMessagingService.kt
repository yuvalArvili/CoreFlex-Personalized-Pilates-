package com.example.coreflexpilates.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.coreflexpilates.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Called when a new FCM message is received
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Log message details for debugging
        Log.d("FCM", "Message received from: ${remoteMessage.from}")
        Log.d("FCM", "Notification data: ${remoteMessage.data}")
        Log.d("FCM", "Notification title: ${remoteMessage.notification?.title}")
        Log.d("FCM", "Notification body: ${remoteMessage.notification?.body}")

        val title = remoteMessage.notification?.title ?: "Reminder"
        val message = remoteMessage.notification?.body ?: "You have a class soon!"
        showNotification(title, message) // Show the notification on device
    }
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
    }

    // Creates and displays a notification with given title and message
    private fun showNotification(title: String, message: String) {
        Log.d("FCM", "Showing notification with title: $title and message: $message")

        val channelId = "default_channel"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "General Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        // Show notification
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
