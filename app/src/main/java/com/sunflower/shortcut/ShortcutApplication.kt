package com.sunflower.shortcut

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.sunflower.shortcut.ai.CodeGenerator
import com.sunflower.shortcut.automation.engine.AutomationEngine
import com.sunflower.shortcut.automation.scheduler.ShortcutWorkerFactory
import com.sunflower.shortcut.data.database.ShortcutDatabase
import com.sunflower.shortcut.data.datastore.SettingsDataStore
import com.sunflower.shortcut.data.repositories.AIHistoryRepository
import com.sunflower.shortcut.data.repositories.AutomationRepository
import com.sunflower.shortcut.data.repositories.LogRepository
import com.sunflower.shortcut.sdk.SdkModuleDispatcher
import com.sunflower.shortcut.sdk.android.AndroidModule
import com.sunflower.shortcut.sdk.apps.AppsModule
import com.sunflower.shortcut.sdk.audio.AudioModule
import com.sunflower.shortcut.sdk.bluetooth.BluetoothModule
import com.sunflower.shortcut.sdk.camera.CameraModule
import com.sunflower.shortcut.sdk.contacts.ContactsModule
import com.sunflower.shortcut.sdk.files.FilesModule
import com.sunflower.shortcut.sdk.location.LocationModule
import com.sunflower.shortcut.sdk.media.MediaModule
import com.sunflower.shortcut.sdk.network.NetworkModule
import com.sunflower.shortcut.sdk.notifications.NotificationsModule
import com.sunflower.shortcut.sdk.phone.PhoneModule
import com.sunflower.shortcut.sdk.sensors.SensorsModule
import com.sunflower.shortcut.sdk.system.SystemModule
import com.sunflower.shortcut.sdk.ui.UiModule
import com.sunflower.shortcut.sdk.wifi.WifiModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Root Application class.
 *
 * Design rule (spec §28/§29/§41 — battery & idle-CPU budget):
 * nothing here does eager *blocking* disk/DB/WorkManager/Accessibility/JS-
 * runtime work. onCreate() does launch one fire-and-forget coroutine to
 * restore trigger listeners (cheap: one DB read + dynamic receiver
 * registration), but everything else stays `by lazy`, created the first
 * time a BroadcastReceiver or the UI actually needs it.
 *
 * This class is the app's manual composition root. No DI framework is used
 * on purpose — the object graph is small and a framework would add APK size
 * and startup cost for no real benefit here (spec §30).
 */
class ShortcutApplication : Application(), Configuration.Provider {

    /** App-wide scope. SupervisorJob so one failed/crashed automation does not
     *  cancel sibling coroutines (scenario isolation — spec §37). */
    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: ShortcutDatabase by lazy {
        ShortcutDatabase.getInstance(applicationContext)
    }

    val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(applicationContext)
    }

    val automationRepository: AutomationRepository by lazy {
        AutomationRepository(
            dao = database.automationDao(),
            context = applicationContext
        )
    }

    val aiHistoryRepository: AIHistoryRepository by lazy {
        AIHistoryRepository(database.aiHistoryDao())
    }

    val logRepository: LogRepository by lazy {
        LogRepository(database.executionLogDao())
    }

    /** Single shared Automation Engine instance (spec §44 — "единый Automation
     *  Engine"). It does not start a JS Runtime on creation — the runtime is
     *  spun up per-execution and torn down right after (spec §28).
     *
     *  The dispatcher only lists modules that actually exist yet — an
     *  automation calling e.g. `camera.*` before that module is built gets a
     *  clear "модуль пока не поддерживается" from SdkModuleDispatcher rather
     *  than the engine's blanket "не инициализированы" message. New modules
     *  join this list as they're generated; nothing else here changes. */
    val automationEngine: AutomationEngine by lazy {
        AutomationEngine(
            context = applicationContext,
            automationRepository = automationRepository,
            logRepository = logRepository,
            scope = applicationScope
        ).also { engine ->
            engine.setNativeBridgeDispatcher(
                SdkModuleDispatcher(
                    listOf(
                        AndroidModule(applicationContext),
                        AppsModule(applicationContext),
                        UiModule(),
                        CameraModule(applicationContext),
                        LocationModule(applicationContext),
                        FilesModule(applicationContext),
                        NetworkModule(),
                        NotificationsModule(applicationContext),
                        AudioModule(applicationContext),
                        BluetoothModule(applicationContext),
                        WifiModule(applicationContext),
                        SensorsModule(applicationContext),
                        MediaModule(applicationContext),
                        ContactsModule(applicationContext),
                        PhoneModule(applicationContext),
                        SystemModule(applicationContext)
                    )
                )
            )
        }
    }

    /**
     * AI Layer entry point (spec §14/§31 — "AI Layer не должен быть связан
     * напрямую с Android execution layer"). It only turns a prompt into JS
     * text via the user's chosen AIProvider; the resulting JS still has to
     * pass through `automationEngine.analyze()` like any imported file.
     */
    val codeGenerator: CodeGenerator by lazy {
        CodeGenerator(settingsDataStore)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Restores trigger listeners for already-installed automations on
        // every normal process start — not just after BOOT_COMPLETED
        // (android.receivers.BootCompletedReceiver handles that separate
        // case, respecting the launchOnBoot setting). Without this, triggers
        // would only ever be active right after a reboot. Fire-and-forget:
        // one cheap DB read + dynamic receiver registration, not the "heavy
        // work" spec §29 warns against — everything else here stays lazy.
        applicationScope.launch { automationEngine.start() }
    }

    /**
     * On-demand WorkManager initialization (spec §30 — no extra init cost
     * unless something actually schedules work). Requires the default
     * WorkManagerInitializer ContentProvider to be removed in
     * AndroidManifest.xml (`tools:node="remove"` on its <provider> entry) —
     * a note for whoever generates the manifest, not something this class
     * can enforce itself.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .setWorkerFactory(
                ShortcutWorkerFactory(
                    logRepository = logRepository,
                    automationEngineProvider = { automationEngine }
                )
            )
            .build()

    companion object {
        lateinit var instance: ShortcutApplication
            private set
    }
}
