package com.pixelbot.memory.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index
import kotlinx.serialization.Serializable
import java.util.Date

@Entity(tableName = "profile", indices = [Index(value = ["key"], unique = true)])
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "contacts", indices = [Index(value = ["nickname"], unique = true)])
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "nickname") val nickname: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    @ColumnInfo(name = "phone_number") val phoneNumber: String,
    @ColumnInfo(name = "photo_uri") val photoUri: String? = null,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_apps", indices = [Index(value = ["package_name"], unique = true)])
data class FavoriteAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "icon_uri") val iconUri: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "command_text") val commandText: String,
    @ColumnInfo(name = "interpreted_action") val interpretedAction: String?,
    @ColumnInfo(name = "success") val success: Boolean,
    @ColumnInfo(name = "error_message") val errorMessage: String? = null,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "routines", indices = [Index(value = ["name"], unique = true)])
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "trigger_type") val triggerType: String, // "voice", "time", "location", "charger"
    @ColumnInfo(name = "trigger_value") val triggerValue: String, // JSON con detalles del trigger
    @ColumnInfo(name = "actions_json") val actionsJson: String, // Lista serializada de ToolCall
    @ColumnInfo(name = "enabled") val enabled: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_run_at") val lastRunAt: Long = 0
)

@Entity(tableName = "routine_actions")
data class RoutineActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "routine_id") val routineId: Long,
    @ColumnInfo(name = "tool_name") val toolName: String,
    @ColumnInfo(name = "args_json") val argsJson: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "wait_for_result") val waitForResult: Boolean = true
)

@Serializable
data class ToolCall(
    val tool: String,
    val args: Map<String, Any>
)