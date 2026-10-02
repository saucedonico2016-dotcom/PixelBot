package com.pixelbot.memory.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.pixelbot.memory.entity.CommandHistoryEntity
import com.pixelbot.memory.entity.ContactEntity
import com.pixelbot.memory.entity.FavoriteAppEntity
import com.pixelbot.memory.entity.ProfileEntity
import com.pixelbot.memory.entity.RoutineActionEntity
import com.pixelbot.memory.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE key = :key")
    fun get(key: String): ProfileEntity?
    
    @Query("SELECT * FROM profile")
    fun getAll(): List<ProfileEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(profiles: List<ProfileEntity>)
    
    @Delete
    suspend fun delete(profile: ProfileEntity)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE nickname = :nickname")
    fun getByNickname(nickname: String): ContactEntity?
    
    @Query("SELECT * FROM contacts WHERE phone_number = :phone")
    fun getByPhone(phone: String): ContactEntity?
    
    @Query("SELECT * FROM contacts ORDER BY is_favorite DESC, nickname ASC")
    fun getAll(): Flow<List<ContactEntity>>
    
    @Query("SELECT * FROM contacts WHERE is_favorite = 1")
    fun getFavorites(): Flow<List<ContactEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: ContactEntity)
    
    @Update
    suspend fun update(contact: ContactEntity)
    
    @Delete
    suspend fun delete(contact: ContactEntity)
}

@Dao
interface FavoriteAppDao {
    @Query("SELECT * FROM favorite_apps ORDER BY sort_order ASC, label ASC")
    fun getAll(): Flow<List<FavoriteAppEntity>>
    
    @Query("SELECT * FROM favorite_apps WHERE package_name = :pkg")
    fun getByPackage(pkg: String): FavoriteAppEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: FavoriteAppEntity)
    
    @Update
    suspend fun update(app: FavoriteAppEntity)
    
    @Delete
    suspend fun delete(app: FavoriteAppEntity)
}

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<CommandHistoryEntity>>
    
    @Query("SELECT * FROM command_history WHERE timestamp > :since ORDER BY timestamp DESC")
    fun getSince(since: Long): Flow<List<CommandHistoryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(history: CommandHistoryEntity)
    
    @Query("DELETE FROM command_history WHERE timestamp < :before")
    suspend fun deleteOld(before: Long)
    
    @Query("SELECT COUNT(*) FROM command_history WHERE success = 1")
    fun getSuccessCount(): Int
    
    @Query("SELECT COUNT(*) FROM command_history WHERE success = 0")
    fun getErrorCount(): Int
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines WHERE name = :name")
    fun getByName(name: String): RoutineEntity?
    
    @Query("SELECT * FROM routines WHERE enabled = 1 ORDER BY name ASC")
    fun getEnabled(): Flow<List<RoutineEntity>>
    
    @Query("SELECT * FROM routines ORDER BY created_at DESC")
    fun getAll(): Flow<List<RoutineEntity>>
    
    @Query("SELECT * FROM routines WHERE trigger_type = :type AND enabled = 1")
    fun getByTriggerType(type: String): Flow<List<RoutineEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(routine: RoutineEntity): Long
    
    @Update
    suspend fun update(routine: RoutineEntity)
    
    @Delete
    suspend fun delete(routine: RoutineEntity)
    
    @Query("UPDATE routines SET last_run_at = :now WHERE id = :id")
    suspend fun updateLastRun(id: Long, now: Long)
}

@Dao
interface RoutineActionDao {
    @Query("SELECT * FROM routine_actions WHERE routine_id = :routineId ORDER BY order_index ASC")
    fun getByRoutine(routineId: Long): Flow<List<RoutineActionEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(actions: List<RoutineActionEntity>)
    
    @Delete
    suspend fun delete(action: RoutineActionEntity)
    
    @Query("DELETE FROM routine_actions WHERE routine_id = :routineId")
    suspend fun deleteByRoutine(routineId: Long)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM profile WHERE key = :key")
    fun getSetting(key: String): ProfileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(key: String, value: String)
    
    @Query("DELETE FROM profile WHERE key = :key")
    suspend fun deleteSetting(key: String)
}