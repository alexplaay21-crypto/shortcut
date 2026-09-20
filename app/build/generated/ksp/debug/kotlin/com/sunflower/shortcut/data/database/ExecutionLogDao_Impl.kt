package com.sunflower.shortcut.`data`.database

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.sunflower.shortcut.`data`.models.ExecutionLogEntity
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
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ExecutionLogDao_Impl(
  __db: RoomDatabase,
) : ExecutionLogDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfExecutionLogEntity: EntityInsertAdapter<ExecutionLogEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfExecutionLogEntity = object : EntityInsertAdapter<ExecutionLogEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `execution_logs` (`id`,`automationId`,`automationTitle`,`startedAtEpochMs`,`triggerReason`,`success`,`durationMs`,`errorMessage`,`errorStep`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ExecutionLogEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.automationId)
        statement.bindText(3, entity.automationTitle)
        statement.bindLong(4, entity.startedAtEpochMs)
        statement.bindText(5, entity.triggerReason)
        val _tmp: Int = if (entity.success) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.durationMs)
        val _tmpErrorMessage: String? = entity.errorMessage
        if (_tmpErrorMessage == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpErrorMessage)
        }
        val _tmpErrorStep: Int? = entity.errorStep
        if (_tmpErrorStep == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpErrorStep.toLong())
        }
      }
    }
  }

  public override suspend fun insert(entry: ExecutionLogEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfExecutionLogEntity.insert(_connection, entry)
  }

  public override fun observeRecent(limit: Int): Flow<List<ExecutionLogEntity>> {
    val _sql: String = "SELECT * FROM execution_logs ORDER BY startedAtEpochMs DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("execution_logs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAutomationId: Int = getColumnIndexOrThrow(_stmt, "automationId")
        val _columnIndexOfAutomationTitle: Int = getColumnIndexOrThrow(_stmt, "automationTitle")
        val _columnIndexOfStartedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "startedAtEpochMs")
        val _columnIndexOfTriggerReason: Int = getColumnIndexOrThrow(_stmt, "triggerReason")
        val _columnIndexOfSuccess: Int = getColumnIndexOrThrow(_stmt, "success")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "errorMessage")
        val _columnIndexOfErrorStep: Int = getColumnIndexOrThrow(_stmt, "errorStep")
        val _result: MutableList<ExecutionLogEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExecutionLogEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpAutomationId: String
          _tmpAutomationId = _stmt.getText(_columnIndexOfAutomationId)
          val _tmpAutomationTitle: String
          _tmpAutomationTitle = _stmt.getText(_columnIndexOfAutomationTitle)
          val _tmpStartedAtEpochMs: Long
          _tmpStartedAtEpochMs = _stmt.getLong(_columnIndexOfStartedAtEpochMs)
          val _tmpTriggerReason: String
          _tmpTriggerReason = _stmt.getText(_columnIndexOfTriggerReason)
          val _tmpSuccess: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSuccess).toInt()
          _tmpSuccess = _tmp != 0
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          val _tmpErrorStep: Int?
          if (_stmt.isNull(_columnIndexOfErrorStep)) {
            _tmpErrorStep = null
          } else {
            _tmpErrorStep = _stmt.getLong(_columnIndexOfErrorStep).toInt()
          }
          _item = ExecutionLogEntity(_tmpId,_tmpAutomationId,_tmpAutomationTitle,_tmpStartedAtEpochMs,_tmpTriggerReason,_tmpSuccess,_tmpDurationMs,_tmpErrorMessage,_tmpErrorStep)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<ExecutionLogEntity> {
    val _sql: String = "SELECT * FROM execution_logs"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAutomationId: Int = getColumnIndexOrThrow(_stmt, "automationId")
        val _columnIndexOfAutomationTitle: Int = getColumnIndexOrThrow(_stmt, "automationTitle")
        val _columnIndexOfStartedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "startedAtEpochMs")
        val _columnIndexOfTriggerReason: Int = getColumnIndexOrThrow(_stmt, "triggerReason")
        val _columnIndexOfSuccess: Int = getColumnIndexOrThrow(_stmt, "success")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "errorMessage")
        val _columnIndexOfErrorStep: Int = getColumnIndexOrThrow(_stmt, "errorStep")
        val _result: MutableList<ExecutionLogEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExecutionLogEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpAutomationId: String
          _tmpAutomationId = _stmt.getText(_columnIndexOfAutomationId)
          val _tmpAutomationTitle: String
          _tmpAutomationTitle = _stmt.getText(_columnIndexOfAutomationTitle)
          val _tmpStartedAtEpochMs: Long
          _tmpStartedAtEpochMs = _stmt.getLong(_columnIndexOfStartedAtEpochMs)
          val _tmpTriggerReason: String
          _tmpTriggerReason = _stmt.getText(_columnIndexOfTriggerReason)
          val _tmpSuccess: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSuccess).toInt()
          _tmpSuccess = _tmp != 0
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          val _tmpErrorStep: Int?
          if (_stmt.isNull(_columnIndexOfErrorStep)) {
            _tmpErrorStep = null
          } else {
            _tmpErrorStep = _stmt.getLong(_columnIndexOfErrorStep).toInt()
          }
          _item = ExecutionLogEntity(_tmpId,_tmpAutomationId,_tmpAutomationTitle,_tmpStartedAtEpochMs,_tmpTriggerReason,_tmpSuccess,_tmpDurationMs,_tmpErrorMessage,_tmpErrorStep)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteByIds(ids: List<String>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM execution_logs WHERE id IN (")
    val _inputSize: Int = ids.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in ids) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
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
