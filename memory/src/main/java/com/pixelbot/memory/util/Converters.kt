package com.pixelbot.memory.util

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import com.pixelbot.memory.entity.ToolCall

class Converters {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @TypeConverter
    fun fromToolCallList(value: String?): List<ToolCall> {
        return value?.let { json.decodeFromString<List<ToolCall>>(it) } ?: emptyList()
    }
    
    @TypeConverter
    fun toToolCallList(list: List<ToolCall>?): String? {
        return list?.let { json.encodeToString(it) }
    }
    
    @TypeConverter
    fun fromMap(value: String?): Map<String, Any> {
        return value?.let { json.decodeFromString<Map<String, Any>>(it) } ?: emptyMap()
    }
    
    @TypeConverter
    fun toMap(map: Map<String, Any>?): String? {
        return map?.let { json.encodeToString(it) }
    }
    
    @TypeConverter
    fun fromList(value: String?): List<String> {
        return value?.let { json.decodeFromString<List<String>>(it) } ?: emptyList()
    }
    
    @TypeConverter
    fun toList(list: List<String>?): String? {
        return list?.let { json.encodeToString(it) }
    }
}