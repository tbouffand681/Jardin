package com.jardin.semis.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jardin.semis.R
import com.jardin.semis.SemisApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class HarvestReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "harvest_reminders"
        const val CHANNEL_NAME = "Rappels de récolte"
    }

    override suspend fun doWork(): Result {
        createNotificationChannel()

        val repository = (applicationContext as SemisApplication).repository
        val activeSowings = repository.getActiveSowings().first()
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        val soonToHarvest = activeSowings.filter {
            val harvestDate = LocalDate.parse(it.expectedHarvestDate, formatter)
            val daysLeft = ChronoUnit.DAYS.between(today, harvestDate)
            daysLeft in 0..3
        }

        if (soonToHarvest.isNotEmpty()) {
            val msg = if (soonToHarvest.size == 1) {
                "1 culture est prête à être récoltée !"
            } else {
                "${soonToHarvest.size} cultures sont bientôt à récolter."
            }
            sendNotification(msg)
        }

        return Result.success()
    }

    private fun sendNotification(message: String) {
        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_plant_notification)
            .setContentTitle("🌿 SemisJardin — Récolte imminente")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Rappels pour les récoltes imminentes"
            }
            val manager = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
