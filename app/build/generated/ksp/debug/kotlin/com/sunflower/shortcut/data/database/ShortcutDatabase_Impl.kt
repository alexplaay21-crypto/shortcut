package com.sunflower.shortcut.`data`.database

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ShortcutDatabase_Impl : ShortcutDatabase() {
  private val _automationDao: Lazy<AutomationDao> = lazy {
    AutomationDao_Impl(this)
  }

  private val _aIHistoryDao: Lazy<AIHistoryDao> = lazy {
    AIHistoryDao_Impl(this)
  }

  private val _executionLogDao: Lazy<ExecutionLogDao> = lazy {
    ExecutionLogDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "b20f36cfd1648e8628849aedc08cee80", "d3c64f1a4655be8dbfebc7204b988d64") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `automations` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `jsCode` TEXT NOT NULL, `triggerKey` TEXT, `enabled` INTEGER NOT NULL, `actionCount` INTEGER NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, `updatedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `ai_history` (`id` TEXT NOT NULL, `prompt` TEXT NOT NULL, `jsCode` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `execution_logs` (`id` TEXT NOT NULL, `automationId` TEXT NOT NULL, `automationTitle` TEXT NOT NULL, `startedAtEpochMs` INTEGER NOT NULL, `triggerReason` TEXT NOT NULL, `success` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `errorMessage` TEXT, `errorStep` INTEGER, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_execution_logs_automationId_startedAtEpochMs` ON `execution_logs` (`automationId`, `startedAtEpochMs`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b20f36cfd1648e8628849aedc08cee80')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `automations`")
        connection.execSQL("DROP TABLE IF EXISTS `ai_history`")
        connection.execSQL("DROP TABLE IF EXISTS `execution_logs`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsAutomations: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAutomations.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAutomations.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAutomations.put("description", TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAutomations.put("jsCode", TableInfo.Column("jsCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAutomations.put("triggerKey", TableInfo.Column("triggerKey", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAutomations.put("enabled", TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAutomations.put("actionCount", TableInfo.Column("actionCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAutomations.put("createdAtEpochMs", TableInfo.Column("createdAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAutomations.put("updatedAtEpochMs", TableInfo.Column("updatedAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAutomations: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAutomations: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAutomations: TableInfo = TableInfo("automations", _columnsAutomations, _foreignKeysAutomations, _indicesAutomations)
        val _existingAutomations: TableInfo = read(connection, "automations")
        if (!_infoAutomations.equals(_existingAutomations)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |automations(com.sunflower.shortcut.data.models.Automation).
              | Expected:
              |""".trimMargin() + _infoAutomations + """
              |
              | Found:
              |""".trimMargin() + _existingAutomations)
        }
        val _columnsAiHistory: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAiHistory.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAiHistory.put("prompt", TableInfo.Column("prompt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAiHistory.put("jsCode", TableInfo.Column("jsCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAiHistory.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAiHistory.put("description", TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAiHistory.put("status", TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAiHistory.put("createdAtEpochMs", TableInfo.Column("createdAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAiHistory: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAiHistory: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAiHistory: TableInfo = TableInfo("ai_history", _columnsAiHistory, _foreignKeysAiHistory, _indicesAiHistory)
        val _existingAiHistory: TableInfo = read(connection, "ai_history")
        if (!_infoAiHistory.equals(_existingAiHistory)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |ai_history(com.sunflower.shortcut.data.models.AiHistoryEntity).
              | Expected:
              |""".trimMargin() + _infoAiHistory + """
              |
              | Found:
              |""".trimMargin() + _existingAiHistory)
        }
        val _columnsExecutionLogs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsExecutionLogs.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExecutionLogs.put("automationId", TableInfo.Column("automationId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExecutionLogs.put("automationTitle", TableInfo.Column("automationTitle", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExecutionLogs.put("startedAtEpochMs", TableInfo.Column("startedAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExecutionLogs.put("triggerReason", TableInfo.Column("triggerReason", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExecutionLogs.put("success", TableInfo.Column("success", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExecutionLogs.put("durationMs", TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExecutionLogs.put("errorMessage", TableInfo.Column("errorMessage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExecutionLogs.put("errorStep", TableInfo.Column("errorStep", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysExecutionLogs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesExecutionLogs: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesExecutionLogs.add(TableInfo.Index("index_execution_logs_automationId_startedAtEpochMs", false, listOf("automationId", "startedAtEpochMs"), listOf("ASC", "ASC")))
        val _infoExecutionLogs: TableInfo = TableInfo("execution_logs", _columnsExecutionLogs, _foreignKeysExecutionLogs, _indicesExecutionLogs)
        val _existingExecutionLogs: TableInfo = read(connection, "execution_logs")
        if (!_infoExecutionLogs.equals(_existingExecutionLogs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |execution_logs(com.sunflower.shortcut.data.models.ExecutionLogEntity).
              | Expected:
              |""".trimMargin() + _infoExecutionLogs + """
              |
              | Found:
              |""".trimMargin() + _existingExecutionLogs)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "automations", "ai_history", "execution_logs")
  }

  public override fun clearAllTables() {
    super.performClear(false, "automations", "ai_history", "execution_logs")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(AutomationDao::class, AutomationDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(AIHistoryDao::class, AIHistoryDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ExecutionLogDao::class, ExecutionLogDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun automationDao(): AutomationDao = _automationDao.value

  public override fun aiHistoryDao(): AIHistoryDao = _aIHistoryDao.value

  public override fun executionLogDao(): ExecutionLogDao = _executionLogDao.value
}
