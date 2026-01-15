package com.dev.point_of_sale_sistem_with_kotlin_pos.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class SyncScheduler(private val context: Context) {

    fun schedule() {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag("OFFLINE_SYNC")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "OFFLINE_SYNC",
                ExistingWorkPolicy.KEEP,
                request
            )
    }
}
