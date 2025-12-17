package com.fourthwardai.orbit.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fourthwardai.orbit.network.ApiResult
import com.fourthwardai.orbit.repository.ArticleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class ArticleSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ArticleRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("ArticleSyncWorker: started")
        return when (val result = repository.syncDirtyArticles()) {
            is ApiResult.Success -> {
                Timber.d("ArticleSyncWorker: success")
                Result.success()
            }
            is ApiResult.Failure -> {
                Timber.d("ArticleSyncWorker: failure = ${result.error}")
                // Retry for transient network errors; otherwise fail
                Result.retry()
            }
        }
    }
}
