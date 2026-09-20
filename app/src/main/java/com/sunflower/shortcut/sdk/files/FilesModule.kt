package com.sunflower.shortcut.sdk.files

import android.content.Context
import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.jsonOf
import com.sunflower.shortcut.sdk.stringArg
import org.json.JSONArray
import java.io.File

/**
 * `files.*` — scoped to the app's own storage (`getExternalFilesDir` /
 * `filesDir`), which needs no runtime permission at all. Reading/writing
 * other apps' files or shared public storage would require either
 * MANAGE_EXTERNAL_STORAGE (a restricted "special access" permission Google
 * Play scrutinizes heavily) or a per-file Storage Access Framework picker —
 * neither fits a script running unattended, so this module honestly stays
 * inside the sandbox rather than half-implementing broader access.
 */
class FilesModule(private val context: Context) : SdkModule {

    override val name: String = "files"

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "read" -> read(args)
        "write" -> write(args)
        "append" -> append(args)
        "exists" -> exists(args)
        "delete" -> delete(args)
        "list" -> list(args)
        else -> throw SdkException("Метод files.$method не поддерживается")
    }

    private fun baseDir(): File = context.getExternalFilesDir(null) ?: context.filesDir

    /** Resolves a script-supplied path against the app's own storage root
     *  and rejects anything that would escape it via "..". */
    private fun resolve(path: String): File {
        val base = baseDir().canonicalFile
        val requested = File(path)
        val target = if (requested.isAbsolute) requested else File(base, path)
        val canonicalTarget = target.canonicalFile
        if (canonicalTarget != base && !canonicalTarget.path.startsWith(base.path + File.separator)) {
            throw SdkException("Доступ разрешён только к файлам приложения Shortcut")
        }
        return canonicalTarget
    }

    private fun read(args: JSONArray): String {
        val path = args.stringArg(0, "path")
        val file = resolve(path)
        if (!file.exists() || !file.isFile) throw SdkException("Файл \"$path\" не найден")
        return jsonOf(runCatching { file.readText() }.getOrElse { throw SdkException("Не удалось прочитать \"$path\"") })
    }

    private fun write(args: JSONArray): String {
        val path = args.stringArg(0, "path")
        val content = args.stringArg(1, "content")
        val file = resolve(path)
        file.parentFile?.mkdirs()
        runCatching { file.writeText(content) }.getOrElse { throw SdkException("Не удалось записать \"$path\"") }
        return jsonOf(true)
    }

    private fun append(args: JSONArray): String {
        val path = args.stringArg(0, "path")
        val content = args.stringArg(1, "content")
        val file = resolve(path)
        file.parentFile?.mkdirs()
        runCatching { file.appendText(content) }.getOrElse { throw SdkException("Не удалось дописать \"$path\"") }
        return jsonOf(true)
    }

    private fun exists(args: JSONArray): String {
        val path = args.stringArg(0, "path")
        return jsonOf(runCatching { resolve(path).exists() }.getOrDefault(false))
    }

    private fun delete(args: JSONArray): String {
        val path = args.stringArg(0, "path")
        val file = resolve(path)
        return jsonOf(file.exists() && file.delete())
    }

    private fun list(args: JSONArray): String {
        val path = if (args.length() > 0) args.stringArg(0, "path") else ""
        val dir = resolve(path)
        if (!dir.isDirectory) throw SdkException("\"$path\" не является папкой")
        val array = JSONArray()
        dir.listFiles()?.sortedBy { it.name }?.forEach { array.put(it.name) }
        return array.toString()
    }
}
