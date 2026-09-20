package com.sunflower.shortcut.sharing

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Sends a scenario to another app as a plain ".js" file (spec §33/§34 — no
 * proprietary wrapper; the receiving Shortcut opens it through MainActivity's
 * ACTION_VIEW / ACTION_SEND handling).
 *
 * Route: write the file into the app's cache → hand out a content:// Uri
 * through FileProvider → system share sheet. If that route fails for any
 * reason (disk full, FileProvider not configured) the code is shared as plain
 * text instead, so the user is never left with a dead button.
 *
 * Requires in the manifest a FileProvider with authority
 * `<applicationId>.fileprovider` (see [fileProviderAuthority]) whose paths
 * XML contains `<cache-path name="shared_scenarios" path="shared/" />`.
 */
object ScenarioSharer {

    private const val TAG = "ScenarioSharer"
    private const val SHARE_DIR = "shared"
    private const val MIME_JS = "application/javascript"
    private const val CHOOSER_TITLE = "Поделиться сценарием"
    private const val STALE_AFTER_MS = 24L * 60 * 60 * 1000

    fun fileProviderAuthority(context: Context): String = "${context.packageName}.fileprovider"

    /**
     * Opens the share sheet. Never throws for expected failures — callers
     * launch this from UI coroutine scopes, where an exception would crash the
     * app. Returns whether a share sheet was actually shown.
     */
    suspend fun shareJs(context: Context, fileName: String, content: String): Boolean {
        val safeName = safeFileName(fileName)

        val fileIntent = withContext(Dispatchers.IO) {
            try {
                fileShareIntent(context, safeName, content)
            } catch (e: Exception) {
                Log.w(TAG, "Не удалось поделиться файлом, отправляю текстом", e)
                null
            }
        }

        return launchChooser(context, fileIntent ?: textShareIntent(safeName, content))
    }

    private fun fileShareIntent(context: Context, fileName: String, content: String): Intent {
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        deleteStaleFiles(dir)

        val file = File(dir, fileName)
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, fileProviderAuthority(context), file)

        return Intent(Intent.ACTION_SEND).apply {
            type = MIME_JS
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            // The chooser only passes the read grant on to the picked app if
            // the Uri is also in the ClipData.
            clipData = ClipData.newRawUri(fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun textShareIntent(fileName: String, content: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            putExtra(Intent.EXTRA_TEXT, content)
        }

    private suspend fun launchChooser(context: Context, target: Intent): Boolean =
        withContext(Dispatchers.Main.immediate) {
            try {
                val chooser = Intent.createChooser(target, CHOOSER_TITLE)
                // From a non-Activity context Android refuses to start an activity without this.
                if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                true
            } catch (e: Exception) {
                // Includes TransactionTooLargeException for an oversized text fallback.
                Log.w(TAG, "Не удалось открыть выбор приложения", e)
                false
            }
        }

    /** Keeps the cache from collecting old exports; the last day's files stay for slow receivers. */
    private fun deleteStaleFiles(dir: File) {
        val now = System.currentTimeMillis()
        dir.listFiles()
            ?.filter { now - it.lastModified() > STALE_AFTER_MS }
            ?.forEach { it.delete() }
    }

    /** Names come from ScenarioExporter, but never trust a name used as a path. */
    private fun safeFileName(fileName: String): String {
        val base = File(fileName).name.ifBlank { "automation.js" }
        return if (base.endsWith(".js", ignoreCase = true)) base else "$base.js"
    }
}
