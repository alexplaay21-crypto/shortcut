package com.sunflower.shortcut.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sunflower.shortcut.data.models.AiHistoryEntity
import com.sunflower.shortcut.data.models.Automation
import com.sunflower.shortcut.data.models.ExecutionLogEntity

/**
 * The app's single Room database (spec §30 — no DI framework; the composition
 * root in ShortcutApplication holds the one instance).
 *
 * Building it is lazy and cheap: Room opens the file on the first actual
 * query, so nothing here touches the disk during process start (spec §29).
 *
 * Schema changes: bump [version] and add a real `Migration`. There is
 * deliberately no fallbackToDestructiveMigration() — it would silently wipe
 * the user's installed scenarios on upgrade.
 */
@Database(
    entities = [
        Automation::class,
        AiHistoryEntity::class,
        ExecutionLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ShortcutDatabase : RoomDatabase() {

    abstract fun automationDao(): AutomationDao

    abstract fun aiHistoryDao(): AIHistoryDao

    abstract fun executionLogDao(): ExecutionLogDao

    companion object {
        private const val DATABASE_NAME = "shortcut.db"

        @Volatile
        private var instance: ShortcutDatabase? = null

        fun getInstance(context: Context): ShortcutDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShortcutDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
    }
}
