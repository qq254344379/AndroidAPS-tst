package app.aaps.plugins.sync.nsclientV3.workers

import android.content.Context
import androidx.work.WorkerParameters
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.objects.workflow.LoggingWorker
import app.aaps.plugins.sync.nsclientV3.NSClientV3Plugin
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * Skeleton worker for the NS `settings` collection.
 *
 * The transport layer (Retrofit + NSAndroidClient) is functional, but no payload
 * or identifier convention is defined yet. Until that is decided, this worker
 * only logs and returns success so the work chain stays structurally complete.
 */
class LoadSettingsWorker(
    context: Context,
    params: WorkerParameters
) : LoggingWorker(context, params, Dispatchers.IO) {

    @Inject lateinit var nsClientV3Plugin: NSClientV3Plugin

    override suspend fun doWorkAndLog(): Result {
        nsClientV3Plugin.nsAndroidClient ?: return Result.success()
        aapsLogger.debug(LTag.NSCLIENT, "LoadSettingsWorker: no-op (payload not defined yet)")
        return Result.success()
    }
}
