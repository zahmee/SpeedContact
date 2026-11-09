package com.speedcontact.app.widget

import android.content.Context
import android.provider.ContactsContract
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.speedcontact.app.app
import java.time.Duration

/**
 * Keeps widgets in step with the phone's contacts: photos, names, numbers and favorites.
 * A content trigger only fires once, so each run schedules the next one.
 */
class ContactsChangedWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        applicationContext.app.widgetStateLoader.requestRefresh()
        applicationContext.app.widgetUpdater.updateAll()
        enqueue(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "contacts-changed"

        fun ensureScheduled(context: Context) = enqueue(context, ExistingWorkPolicy.KEEP)

        /** One kind running out of widgets must not stop updates for the other kind. */
        fun cancelIfUnused(context: Context) {
            if (!WidgetKind.anyPlaced(context)) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            }
        }

        private fun enqueue(context: Context, policy: ExistingWorkPolicy) {
            val constraints = Constraints.Builder()
                .addContentUriTrigger(ContactsContract.Contacts.CONTENT_URI, true)
                .setTriggerContentUpdateDelay(Duration.ofSeconds(5))
                .setTriggerContentMaxDelay(Duration.ofMinutes(1))
                .build()
            val request = OneTimeWorkRequestBuilder<ContactsChangedWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
        }
    }
}
