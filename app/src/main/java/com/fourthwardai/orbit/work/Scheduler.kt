package com.fourthwardai.orbit.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

fun scheduleArticleSync(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    val workRequest = OneTimeWorkRequestBuilder<ArticleSyncWorker>()
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
        .addTag("sync_articles")
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "sync_articles",
        ExistingWorkPolicy.APPEND,
        workRequest,
    )
}
