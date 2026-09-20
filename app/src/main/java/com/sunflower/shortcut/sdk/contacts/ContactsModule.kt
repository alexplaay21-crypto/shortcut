package com.sunflower.shortcut.sdk.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.jsonObjectOf
import com.sunflower.shortcut.sdk.stringArg
import org.json.JSONArray

private const val FIND_LIMIT = 50
private const val GET_ALL_LIMIT = 200

/**
 * `contacts.find(query)` / `contacts.getAll()`. Result counts are capped —
 * not a fake restriction, a real safeguard against handing a script (and
 * whatever it does next, e.g. a template string built from it) a
 * multi-thousand-entry JSON blob from someone's full address book.
 */
class ContactsModule(private val context: Context) : SdkModule {

    override val name: String = "contacts"

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "find" -> find(args)
        "getAll" -> getAll()
        else -> throw SdkException("Метод contacts.$method не поддерживается")
    }

    private fun ensurePermission() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) throw SdkException("Нет разрешения на доступ к контактам")
    }

    private fun find(args: JSONArray): String {
        ensurePermission()
        val query = args.stringArg(0, "query")
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        return queryPhones(selection, selectionArgs, FIND_LIMIT, dedupe = false)
    }

    private fun getAll(): String {
        ensurePermission()
        return queryPhones(
            selection = null,
            selectionArgs = null,
            limit = GET_ALL_LIMIT,
            dedupe = true,
            sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )
    }

    private fun queryPhones(
        selection: String?,
        selectionArgs: Array<String>?,
        limit: Int,
        dedupe: Boolean,
        sortOrder: String? = null
    ): String {
        val results = JSONArray()
        val seen = mutableSetOf<String>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, selection, selectionArgs, sortOrder
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext() && results.length() < limit) {
                val contactName = cursor.getString(nameIndex) ?: continue
                val number = cursor.getString(numberIndex) ?: continue
                if (dedupe && !seen.add("$contactName|$number")) continue
                results.put(jsonObjectOf("name" to contactName, "phone" to number))
            }
        }
        return results.toString()
    }
}
