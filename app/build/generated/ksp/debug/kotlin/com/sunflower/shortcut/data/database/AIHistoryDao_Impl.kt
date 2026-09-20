package com.sunflower.shortcut.`data`.database

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.sunflower.shortcut.`data`.models.AiHistoryEntity
import javax.`annotation`.processing.Generated
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
public class AIHistoryDao_Impl(
  __db: RoomDatabase,
) : AIHistoryDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfAiHistoryEntity: EntityInsertAdapter<AiHistoryEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfAiHistoryEntity = object : EntityInsertAdapter<AiHistoryEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `ai_history` (`id`,`prompt`,`jsCode`,`title`,`description`,`status`,`createdAtEpochMs`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: AiHistoryEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.prompt)
        statement.bindText(3, entity.jsCode)
        statement.bindText(4, entity.title)
        statement.bindText(5, entity.description)
        statement.bindText(6, entity.status)
        statement.bindLong(7, entity.createdAtEpochMs)
      }
    }
  }

  public override suspend fun insert(entry: AiHistoryEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfAiHistoryEntity.insert(_connection, entry)
  }

  public override fun observeAll(): Flow<List<AiHistoryEntity>> {
    val _sql: String = "SELECT * FROM ai_history ORDER BY createdAtEpochMs DESC"
    return createFlow(__db, false, arrayOf("ai_history")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPrompt: Int = getColumnIndexOrThrow(_stmt, "prompt")
        val _columnIndexOfJsCode: Int = getColumnIndexOrThrow(_stmt, "jsCode")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _result: MutableList<AiHistoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AiHistoryEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpPrompt: String
          _tmpPrompt = _stmt.getText(_columnIndexOfPrompt)
          val _tmpJsCode: String
          _tmpJsCode = _stmt.getText(_columnIndexOfJsCode)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          _item = AiHistoryEntity(_tmpId,_tmpPrompt,_tmpJsCode,_tmpTitle,_tmpDescription,_tmpStatus,_tmpCreatedAtEpochMs)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: String): AiHistoryEntity? {
    val _sql: String = "SELECT * FROM ai_history WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPrompt: Int = getColumnIndexOrThrow(_stmt, "prompt")
        val _columnIndexOfJsCode: Int = getColumnIndexOrThrow(_stmt, "jsCode")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _result: AiHistoryEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpPrompt: String
          _tmpPrompt = _stmt.getText(_columnIndexOfPrompt)
          val _tmpJsCode: String
          _tmpJsCode = _stmt.getText(_columnIndexOfJsCode)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          _result = AiHistoryEntity(_tmpId,_tmpPrompt,_tmpJsCode,_tmpTitle,_tmpDescription,_tmpStatus,_tmpCreatedAtEpochMs)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateStatus(id: String, status: String) {
    val _sql: String = "UPDATE ai_history SET status = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, status)
        _argIndex = 2
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
