package com.sunflower.shortcut.sdk.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.jsonObjectOf
import com.sunflower.shortcut.sdk.jsonOf
import com.sunflower.shortcut.sdk.objectArgOrNull
import com.sunflower.shortcut.sdk.stringArg
import org.json.JSONArray

class AppsModule(private val context: Context) : SdkModule {

    override val name: String = "apps"

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "launch" -> launch(args)
        "stop" -> stop()
        "openUrl" -> openUrl(args)
        "getInstalled" -> getInstalled()
        "getInfo" -> getInfo(args)
        "sendIntent" -> sendIntent(args)
        "share" -> share(args)
        else -> throw SdkException("Метод apps.$method не поддерживается")
    }

    private val packageManager get() = context.packageManager

    /** (packageName, label) for every launchable app — queried fresh each
     *  time rather than cached, since installed apps can change between
     *  scenario runs (spec §29 accepts this cost: only paid when apps.* is
     *  actually used, never on an idle path). */
    private fun launchableApps(): List<Pair<String, String>> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0).map { info ->
            info.activityInfo.packageName to info.loadLabel(packageManager).toString()
        }
    }

    /** Accepts either a real package name or a human app label ("Telegram") —
     *  matches spec §15's examples, which always use the display name. */
    private fun resolvePackageName(nameOrPackage: String): String {
        runCatching { packageManager.getApplicationInfo(nameOrPackage, 0) }
            .getOrNull()
            ?.let { return nameOrPackage }

        val launchables = launchableApps()
        launchables.firstOrNull { it.second.equals(nameOrPackage, ignoreCase = true) }?.let { return it.first }
        launchables.firstOrNull { it.second.contains(nameOrPackage, ignoreCase = true) }?.let { return it.first }
        throw SdkException("Приложение \"$nameOrPackage\" не найдено")
    }

    private fun launch(args: JSONArray): String {
        val nameOrPackage = args.stringArg(0, "app")
        val packageName = resolvePackageName(nameOrPackage)
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: throw SdkException("Не удалось получить intent запуска для \"$nameOrPackage\"")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return jsonOf(true)
    }

    /**
     * Honest limitation, not a fake success: Android has not let a regular
     * (non-system, non-device-owner) app force-stop another app since the
     * FORCE_STOP_PACKAGES permission became signature|system-only. There is
     * no workaround that doesn't require root or a device-owner profile.
     */
    private fun stop(): String {
        throw SdkException("Android не позволяет обычным приложениям принудительно останавливать другие приложения")
    }

    private fun openUrl(args: JSONArray): String {
        val url = args.stringArg(0, "url")
        val uri = runCatching { Uri.parse(url) }.getOrNull()
            ?: throw SdkException("Некорректная ссылка: $url")
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { throw SdkException("Не удалось открыть ссылку: $url") }
        return jsonOf(true)
    }

    private fun getInstalled(): String {
        val array = JSONArray()
        launchableApps()
            .distinctBy { it.first }
            .sortedBy { it.second }
            .forEach { (packageName, label) -> array.put(jsonObjectOf("name" to label, "packageName" to packageName)) }
        return array.toString()
    }

    private fun getInfo(args: JSONArray): String {
        val nameOrPackage = args.stringArg(0, "app")
        val packageName = resolvePackageName(nameOrPackage)
        val packageInfo = runCatching { packageManager.getPackageInfo(packageName, 0) }
            .getOrElse { throw SdkException("Не удалось получить информацию о \"$nameOrPackage\"") }
        val appInfo = packageInfo.applicationInfo
        val label = appInfo?.let { packageManager.getApplicationLabel(it).toString() } ?: packageName
        val isSystemApp = appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        return jsonObjectOf(
            "name" to label,
            "packageName" to packageName,
            "versionName" to (packageInfo.versionName ?: ""),
            "isSystemApp" to isSystemApp
        ).toString()
    }

    /** Generic intent launcher (spec §16 apps.sendIntent). Only explicit,
     *  author-supplied fields go into the Intent — no reflection, no
     *  arbitrary class loading (spec §20). */
    private fun sendIntent(args: JSONArray): String {
        val options = args.objectArgOrNull(0) ?: throw SdkException("sendIntent требует объект с параметрами")
        val action = options.optString("action").takeIf { it.isNotBlank() } ?: Intent.ACTION_VIEW
        val intent = Intent(action)
        options.optString("packageName").takeIf { it.isNotBlank() }?.let { intent.setPackage(it) }
        options.optString("data").takeIf { it.isNotBlank() }?.let { intent.data = Uri.parse(it) }
        options.optString("type").takeIf { it.isNotBlank() }?.let { intent.type = it }
        options.optJSONObject("extras")?.let { extras ->
            extras.keys().forEach { key -> intent.putExtra(key, extras.optString(key)) }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { throw SdkException("Не удалось отправить intent: ${it.message}") }
        return jsonOf(true)
    }

    private fun share(args: JSONArray): String {
        val options = args.objectArgOrNull(0) ?: throw SdkException("share требует объект с параметрами")
        val text = options.optString("text")
        val subject = options.optString("subject")
        val mimeType = options.optString("type").takeIf { it.isNotBlank() } ?: "text/plain"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            if (text.isNotBlank()) putExtra(Intent.EXTRA_TEXT, text)
            if (subject.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        val chooser = Intent.createChooser(sendIntent, subject.ifBlank { null })
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
        return jsonOf(true)
    }
}
