package com.sunflower.shortcut.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One installed scenario (spec §5). The stored [jsCode] is the single source
 * of truth — title, description, trigger and action count are only a cache of
 * what the analyzer found at install time, so lists can render without
 * parsing every script.
 *
 * Column names are referenced by raw SQL in AutomationDao, so renaming a
 * property here means updating those queries too.
 *
 * The UI never sees this type; AutomationRepository maps it to AutomationUi.
 */
@Entity(tableName = "automations")
data class Automation(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val jsCode: String,
    /** Raw `on("...")` key, e.g. "charging" — see TriggerType.key. Null if none. */
    val triggerKey: String?,
    val enabled: Boolean,
    val actionCount: Int,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
