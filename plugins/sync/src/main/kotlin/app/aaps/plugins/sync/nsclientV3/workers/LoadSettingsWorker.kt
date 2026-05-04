package app.aaps.plugins.sync.nsclientV3.workers

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.nsclient.NSClientRepository
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.nssdk.interfaces.RunningConfiguration
import app.aaps.core.objects.workflow.LoggingWorker
import app.aaps.plugins.sync.nsclientV3.NSClientV3Plugin
import app.aaps.plugins.sync.nsclientV3.extensions.toRunningConfiguration
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * Catch-up loader for the NS `settings/aaps` document.
 *
 * Master role (`config.APS`): no-op (master publishes via [RunningConfigurationPublisher],
 * applying our own doc back to ourselves would be a feedback loop).
 *
 * Client role (`config.AAPSCLIENT`): GET the doc, extract `runningConfig`, apply it via
 * [RunningConfiguration.apply], advance the settings high-water-mark. Real-time updates
 * after this initial catch-up arrive via the WS `settings` branch in `NSClientV3Service`.
 */
class LoadSettingsWorker(
    context: Context,
    params: WorkerParameters
) : LoggingWorker(context, params, Dispatchers.IO) {

    @Inject lateinit var nsClientV3Plugin: NSClientV3Plugin
    @Inject lateinit var nsClientRepository: NSClientRepository
    @Inject lateinit var runningConfiguration: RunningConfiguration
    @Inject lateinit var config: Config
    @Inject lateinit var dateUtil: DateUtil

    override suspend fun doWorkAndLog(): Result {
        val client = nsClientV3Plugin.nsAndroidClient ?: return Result.success()
        if (!config.AAPSCLIENT) return Result.success() // master skips reading

        try {
            val response = client.getSettings(IDENTIFIER)
            val doc = response.values
            if (doc == null) {
                nsClientRepository.addLog("◄ RCV SETTINGS", "no doc yet")
            } else {
                // ETag isn't set for settings GETs, but srvModified is in the doc body
                val srvModified = doc.optLong("srvModified", 0L).takeIf { it > 0 }
                    ?: response.lastServerModified
                doc.toString().toRunningConfiguration()?.let { configuration ->
                    runningConfiguration.apply(configuration)
                    val ts = srvModified?.let { dateUtil.dateAndTimeAndSecondsString(it) } ?: "?"
                    nsClientRepository.addLog("◄ SETTINGS", "applied srvModified=$ts")
                } ?: nsClientRepository.addLog("◄ SETTINGS", "doc present but missing/invalid runningConfig")
                srvModified?.let {
                    nsClientV3Plugin.lastLoadedSrvModified.set("settings", it)
                    nsClientV3Plugin.storeLastLoadedSrvModified()
                }
            }
        } catch (e: Exception) {
            aapsLogger.error(LTag.NSCLIENT, "LoadSettingsWorker failed", e)
            nsClientRepository.addLog("◄ ERROR SETTINGS", e.localizedMessage ?: e.toString())
            nsClientV3Plugin.lastOperationError = e.localizedMessage
            return Result.failure(workDataOf("Error" to e.localizedMessage))
        }
        nsClientV3Plugin.lastOperationError = null
        return Result.success()
    }

    private companion object {

        const val IDENTIFIER = "aaps"
    }
}
