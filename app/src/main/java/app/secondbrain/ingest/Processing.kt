package app.secondbrain.ingest

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.secondbrain.data.db.ItemDao
import app.secondbrain.data.db.ItemEntity
import app.secondbrain.data.db.ProcessingStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@HiltWorker
class ProcessItemWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val processor: ItemProcessor,
    private val dao: ItemDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1)
        if (id < 0) return Result.failure()
        return try {
            processor.process(id)
            Result.success()
        } catch (t: Throwable) {
            // The item is already saved; park it as FAILED so it stays visible and retryable.
            dao.get(id)?.let { dao.update(it.copy(status = ProcessingStatus.FAILED)) }
            Result.success()
        }
    }

    companion object {
        const val KEY_ID = "item_id"
    }
}

@Singleton
class ProcessingScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ItemDao,
) {
    fun enqueue(id: Long) {
        val request = OneTimeWorkRequestBuilder<ProcessItemWorker>()
            .setInputData(workDataOf(ProcessItemWorker.KEY_ID to id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("process-$id", ExistingWorkPolicy.REPLACE, request)
    }

    /** Picks up anything a previous session left unfinished (app killed, offline link, etc.). */
    suspend fun resumeUnprocessed() {
        dao.getUnprocessed().map(ItemEntity::id).forEach(::enqueue)
    }
}
