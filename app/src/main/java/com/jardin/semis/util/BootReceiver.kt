package com.jardin.semis.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleHarvestReminder(context)
        }
    }
}

fun scheduleHarvestReminder(context: Context) {
    val request = PeriodicWorkRequestBuilder<HarvestReminderWorker>(1, TimeUnit.DAYS)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "harvest_reminder",
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}
