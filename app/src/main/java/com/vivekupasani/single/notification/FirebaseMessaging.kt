package com.vivekupasani.single.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vivekupasani.single.MainActivity // Import your MainActivity
import com.vivekupasani.single.R
import java.util.*

class FirebaseMessaging : FirebaseMessagingService() {

    private val channelID = "Post-Notification"
    private val channelName = "Post-Notification"

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToFirestore(token)
    }

    private fun saveTokenToFirestore(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Update the user's token field in Firestore
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("Users")
                .document(currentUser.uid)
                .update("token", token)
                .addOnSuccessListener {
                    Log.d("FCM", "Token saved to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "Failed to save token: ${e.message}")
                }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        // Assuming the profile picture is passed in data payload as URL
        val largeIconBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.single_logo) // Replace with profile image from payload

        // Intent to open MainActivity and navigate to ChatsFragment when notification is clicked
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigateToFragment", "chats") // You can use this to indicate fragment navigation
            putExtra("userId", message.data["userId"]) // Passing necessary data like userId
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // WhatsApp-like notification with large icon
        val builder = NotificationCompat.Builder(applicationContext, channelID)
            .setSmallIcon(R.drawable.single_logo) // Use your actual icon resource
            .setLargeIcon(largeIconBitmap) // Profile picture or sender's picture
            .setColor(ContextCompat.getColor(applicationContext, R.color.black))
            .setContentTitle(message.data["title"]) // Sender's name or group name
            .setContentText(message.data["body"]) // Message content
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // Open MainActivity on tap
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.data["body"])) // Expandable notification for long messages

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check for POST_NOTIFICATIONS permission on Android 13+
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(applicationContext).notify(Random().nextInt(3000), builder.build())
            }
        } else {
            NotificationManagerCompat.from(applicationContext).notify(Random().nextInt(3000), builder.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }
}
