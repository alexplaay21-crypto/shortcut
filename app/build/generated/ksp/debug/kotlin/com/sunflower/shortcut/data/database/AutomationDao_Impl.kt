package com.sunflower.shortcut.`data`.database

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.sunflower.shortcut.`data`.models.Automation
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AutomationDao_Impl(
  __db: RoomDatabase,
) : AutomationDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfAutomation: EntityInsertAdapter<Automation>
  init {
    this.__db = __db
    this.__insertAdapterOfAutomation = object : EntityInsertAdapter<Automation>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `automations` (`id`,`title`,`description`,`jsCode`,`triggerKey`,`enabled`,`actionCount`,`createdAtEpochMs`,`updatedAtEpochMs`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Automation) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.description)
        statement.bindText(4, entity.jsCode)
        val _tmpTriggerKey: String? = entity.triggerKey
        if (_tmpTriggerKey == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpTriggerKey)
        }
        val _tmp: Int = if (entity.enabled) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.actionCount.toLong())
        statement.bindLong(8, entity.createdAtEpochMs)
        statement.bindLong(9, entity.updatedAtEpochMs)
      }
    }
  }

  public override suspend fun insert(automation: Automation): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfAutomation.insert(_connection, automation)
  }

  public override fun observeAll(): Flow<List<AutomationWithLastRun>> {
    val _sql: String = """
        |
        |    SELECT a.*,
        |        (SELECT l.startedAtEpochMs FROM execution_logs l
        |            WHERE l.automationId = a.id
        |            ORDER BY l.startedAtEpochMs DESC LIMIT 1) AS lastRunAtEpochMs,
        |        (SELECT l.success FROM execution_logs l
        |            WHERE l.automationId = a.id
        |            ORDER BY l.startedAtEpochMs DESC LIMIT 1) AS lastRunSuccess
        |    FROM automations a
        | ORDER BY a.createdAtEpochMs DESC
        """.trimMargin()
    return createFlow(__db, false, arrayOf("execution_logs", "automations")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfJsCode: Int = getColumnIndexOrThrow(_stmt, "jsCode")
        val _columnIndexOfTriggerKey: Int = getColumnIndexOrThrow(_stmt, "triggerKey")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfActionCount: Int = getColumnIndexOrThrow(_stmt, "actionCount")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _columnIndexOfUpdatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "updatedAtEpochMs")
        val _columnIndexOfLastRunAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "lastRunAtEpochMs")
        val _columnIndexOfLastRunSuccess: Int = getColumnIndexOrThrow(_stmt, "lastRunSuccess")
        val _result: MutableList<AutomationWithLastRun> = mutableListOf()
        while (_stmt.step()) {
          val _item: AutomationWithLastRun
          val _tmpLastRunAtEpochMs: Long?
          if (_stmt.isNull(_columnIndexOfLastRunAtEpochMs)) {
            _tmpLastRunAtEpochMs = null
          } else {
            _tmpLastRunAtEpochMs = _stmt.getLong(_columnIndexOfLastRunAtEpochMs)
          }
          val _tmpLastRunSuccess: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfLastRunSuccess)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfLastRunSuccess).toInt()
          }
          _tmpLastRunSuccess = _tmp?.let { it != 0 }
          val _tmpAutomation: Automation
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpJsCode: String
          _tmpJsCode = _stmt.getText(_columnIndexOfJsCode)
          val _tmpTriggerKey: String?
          if (_stmt.isNull(_columnIndexOfTriggerKey)) {
            _tmpTriggerKey = null
          } else {
            _tmpTriggerKey = _stmt.getText(_columnIndexOfTriggerKey)
          }
          val _tmpEnabled: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp_1 != 0
          val _tmpActionCount: Int
          _tmpActionCount = _stmt.getLong(_columnIndexOfActionCount).toInt()
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          val _tmpUpdatedAtEpochMs: Long
          _tmpUpdatedAtEpochMs = _stmt.getLong(_columnIndexOfUpdatedAtEpochMs)
          _tmpAutomation = Automation(_tmpId,_tmpTitle,_tmpDescription,_tmpJsCode,_tmpTriggerKey,_tmpEnabled,_tmpActionCount,_tmpCreatedAtEpochMs,_tmpUpdatedAtEpochMs)
          _item = AutomationWithLastRun(_tmpAutomation,_tmpLastRunAtEpochMs,_tmpLastRunSuccess)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeById(id: String): Flow<AutomationWithLastRun?> {
    val _sql: String = """
        |
        |    SELECT a.*,
        |        (SELECT l.startedAtEpochMs FROM execution_logs l
        |            WHERE l.automationId = a.id
        |            ORDER BY l.startedAtEpochMs DESC LIMIT 1) AS lastRunAtEpochMs,
        |        (SELECT l.success FROM execution_logs l
        |            WHERE l.automationId = a.id
        |            ORDER BY l.startedAtEpochMs DESC LIMIT 1) AS lastRunSuccess
        |    FROM automations a
        | WHERE a.id = ?
        """.trimMargin()
    return createFlow(__db, false, arrayOf("execution_logs", "automations")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfJsCode: Int = getColumnIndexOrThrow(_stmt, "jsCode")
        val _columnIndexOfTriggerKey: Int = getColumnIndexOrThrow(_stmt, "triggerKey")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfActionCount: Int = getColumnIndexOrThrow(_stmt, "actionCount")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _columnIndexOfUpdatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "updatedAtEpochMs")
        val _columnIndexOfLastRunAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "lastRunAtEpochMs")
        val _columnIndexOfLastRunSuccess: Int = getColumnIndexOrThrow(_stmt, "lastRunSuccess")
        val _result: AutomationWithLastRun?
        if (_stmt.step()) {
          val _tmpLastRunAtEpochMs: Long?
          if (_stmt.isNull(_columnIndexOfLastRunAtEpochMs)) {
            _tmpLastRunAtEpochMs = null
          } else {
            _tmpLastRunAtEpochMs = _stmt.getLong(_columnIndexOfLastRunAtEpochMs)
          }
          val _tmpLastRunSuccess: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfLastRunSuccess)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfLastRunSuccess).toInt()
          }
          _tmpLastRunSuccess = _tmp?.let { it != 0 }
          val _tmpAutomation: Automation
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpJsCode: String
          _tmpJsCode = _stmt.getText(_columnIndexOfJsCode)
          val _tmpTriggerKey: String?
          if (_stmt.isNull(_columnIndexOfTriggerKey)) {
            _tmpTriggerKey = null
          } else {
            _tmpTriggerKey = _stmt.getText(_columnIndexOfTriggerKey)
          }
          val _tmpEnabled: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp_1 != 0
          val _tmpActionCount: Int
          _tmpActionCount = _stmt.getLong(_columnIndexOfActionCount).toInt()
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          val _tmpUpdatedAtEpochMs: Long
          _tmpUpdatedAtEpochMs = _stmt.getLong(_columnIndexOfUpdatedAtEpochMs)
          _tmpAutomation = Automation(_tmpId,_tmpTitle,_tmpDescription,_tmpJsCode,_tmpTriggerKey,_tmpEnabled,_tmpActionCount,_tmpCreatedAtEpochMs,_tmpUpdatedAtEpochMs)
          _result = AutomationWithLastRun(_tmpAutomation,_tmpLastRunAtEpochMs,_tmpLastRunSuccess)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getByIdOnce(id: String): Automation? {
    val _sql: String = "SELECT * FROM automations WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfJsCode: Int = getColumnIndexOrThrow(_stmt, "jsCode")
        val _columnIndexOfTriggerKey: Int = getColumnIndexOrThrow(_stmt, "triggerKey")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfActionCount: Int = getColumnIndexOrThrow(_stmt, "actionCount")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _columnIndexOfUpdatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "updatedAtEpochMs")
        val _result: Automation?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpJsCode: String
          _tmpJsCode = _stmt.getText(_columnIndexOfJsCode)
          val _tmpTriggerKey: String?
          if (_stmt.isNull(_columnIndexOfTriggerKey)) {
            _tmpTriggerKey = null
          } else {
            _tmpTriggerKey = _stmt.getText(_columnIndexOfTriggerKey)
          }
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpActionCount: Int
          _tmpActionCount = _stmt.getLong(_columnIndexOfActionCount).toInt()
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          val _tmpUpdatedAtEpochMs: Long
          _tmpUpdatedAtEpochMs = _stmt.getLong(_columnIndexOfUpdatedAtEpochMs)
          _result = Automation(_tmpId,_tmpTitle,_tmpDescription,_tmpJsCode,_tmpTriggerKey,_tmpEnabled,_tmpActionCount,_tmpCreatedAtEpochMs,_tmpUpdatedAtEpochMs)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllOnce(): List<Automation> {
    val _sql: String = "SELECT * FROM automations ORDER BY createdAtEpochMs DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfJsCode: Int = getColumnIndexOrThrow(_stmt, "jsCode")
        val _columnIndexOfTriggerKey: Int = getColumnIndexOrThrow(_stmt, "triggerKey")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfActionCount: Int = getColumnIndexOrThrow(_stmt, "actionCount")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _columnIndexOfUpdatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "updatedAtEpochMs")
        val _result: MutableList<Automation> = mutableListOf()
        while (_stmt.step()) {
          val _item: Automation
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpJsCode: String
          _tmpJsCode = _stmt.getText(_columnIndexOfJsCode)
          val _tmpTriggerKey: String?
          if (_stmt.isNull(_columnIndexOfTriggerKey)) {
            _tmpTriggerKey = null
          } else {
            _tmpTriggerKey = _stmt.getText(_columnIndexOfTriggerKey)
          }
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpActionCount: Int
          _tmpActionCount = _stmt.getLong(_columnIndexOfActionCount).toInt()
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          val _tmpUpdatedAtEpochMs: Long
          _tmpUpdatedAtEpochMs = _stmt.getLong(_columnIndexOfUpdatedAtEpochMs)
          _item = Automation(_tmpId,_tmpTitle,_tmpDescription,_tmpJsCode,_tmpTriggerKey,_tmpEnabled,_tmpActionCount,_tmpCreatedAtEpochMs,_tmpUpdatedAtEpochMs)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getCode(id: String): String? {
    val _sql: String = "SELECT jsCode FROM automations WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _result: String?
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null
          } else {
            _result = _stmt.getText(0)
          }
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getTriggerRows(): List<AutomationTriggerRow> {
    val _sql: String = "SELECT id AS automationId, triggerKey FROM automations WHERE triggerKey IS NOT NULL"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfAutomationId: Int = 0
        val _columnIndexOfTriggerKey: Int = 1
        val _result: MutableList<AutomationTriggerRow> = mutableListOf()
        while (_stmt.step()) {
          val _item: AutomationTriggerRow
          val _tmpAutomationId: String
          _tmpAutomationId = _stmt.getText(_columnIndexOfAutomationId)
          val _tmpTriggerKey: String
          _tmpTriggerKey = _stmt.getText(_columnIndexOfTriggerKey)
          _item = AutomationTriggerRow(_tmpAutomationId,_tmpTriggerKey)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun setEnabled(id: String, enabled: Boolean) {
    val _sql: String = "UPDATE automations SET enabled = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (enabled) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateCode(
    id: String,
    jsCode: String,
    updatedAtEpochMs: Long,
  ) {
    val _sql: String = "UPDATE automations SET jsCode = ?, updatedAtEpochMs = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, jsCode)
        _argIndex = 2
        _stmt.bindLong(_argIndex, updatedAtEpochMs)
        _argIndex = 3
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun delete(id: String) {
    val _sql: String = "DELETE FROM automations WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
