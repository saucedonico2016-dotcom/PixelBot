package com.pixelbot.memory

import android.content.Context
import com.pixelbot.memory.dao.CommandHistoryDao
import com.pixelbot.memory.dao.ContactDao
import com.pixelbot.memory.dao.FavoriteAppDao
import com.pixelbot.memory.dao.ProfileDao
import com.pixelbot.memory.dao.RoutineActionDao
import com.pixelbot.memory.dao.RoutineDao
import com.pixelbot.memory.dao.SettingsDao
import com.pixelbot.memory.entity.CommandHistoryEntity
import com.pixelbot.memory.entity.ContactEntity
import com.pixelbot.memory.entity.FavoriteAppEntity
import com.pixelbot.memory.entity.ProfileEntity
import com.pixelbot.memory.entity.RoutineActionEntity
import com.pixelbot.memory.entity.RoutineEntity
import com.pixelbot.memory.entity.ToolCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MemoryModule(private val context: Context) {
    
    private val db = PixelDatabase.getInstance(context)
    
    // Profile
    val profileDao: ProfileDao = db.profileDao()
    val settingsDao: SettingsDao = db.settingsDao()
    
    suspend fun setProfile(key: String, value: String) {
        profileDao.upsert(ProfileEntity(key = key, value = value))
    }
    
    suspend fun getProfile(key: String): String? = profileDao.get(key)?.value
    
    suspend fun getAllProfiles(): List<ProfileEntity> = profileDao.getAll()
    
    // Contacts
    val contactDao: ContactDao = db.contactDao()
    
    suspend fun addContact(nickname: String, fullName: String, phone: String, photoUri: String? = null): Long {
        return contactDao.upsert(ContactEntity(
            nickname = nickname,
            fullName = fullName,
            phoneNumber = phone,
            photoUri = photoUri
        ))
    }
    
    suspend fun getContact(nickname: String): ContactEntity? = contactDao.getByNickname(nickname)
    
    suspend fun getContactByPhone(phone: String): ContactEntity? = contactDao.getByPhone(phone)
    
    val allContacts: Flow<List<ContactEntity>> = contactDao.getAll()
    val favoriteContacts: Flow<List<ContactEntity>> = contactDao.getFavorites()
    
    suspend fun setFavorite(nickname: String, favorite: Boolean) {
        contactDao.getByNickname(nickname)?.let { contact ->
            contactDao.update(contact.copy(isFavorite = favorite))
        }
    }
    
    // Favorite Apps
    val favoriteAppDao: FavoriteAppDao = db.favoriteAppDao()
    
    suspend fun addFavoriteApp(packageName: String, label: String, iconUri: String? = null): Long {
        val count = favoriteAppDao.getAll().firstOrNull()?.size ?: 0
        return favoriteAppDao.upsert(FavoriteAppEntity(
            packageName = packageName,
            label = label,
            iconUri = iconUri,
            sortOrder = count
        ))
    }
    
    val favoriteApps: Flow<List<FavoriteAppEntity>> = favoriteAppDao.getAll()
    
    suspend fun reorderFavoriteApps(apps: List<FavoriteAppEntity>) {
        apps.forEachIndexed { index, app ->
            favoriteAppDao.update(app.copy(sortOrder = index))
        }
    }
    
    // Command History
    val commandHistoryDao: CommandHistoryDao = db.commandHistoryDao()
    
    suspend fun logCommand(
        commandText: String,
        interpretedAction: String?,
        success: Boolean,
        errorMessage: String? = null,
        durationMs: Long
    ) {
        commandHistoryDao.insert(CommandHistoryEntity(
            commandText = commandText,
            interpretedAction = interpretedAction,
            success = success,
            errorMessage = errorMessage,
            durationMs = durationMs
        ))
    }
    
    val recentCommands: Flow<List<CommandHistoryEntity>> = commandHistoryDao.getRecent(50)
    
    suspend fun cleanOldHistory(olderThanDays: Int = 30) {
        val before = System.currentTimeMillis() - (olderThanDays * 24L * 60 * 60 * 1000)
        commandHistoryDao.deleteOld(before)
    }
    
    // Routines
    val routineDao: RoutineDao = db.routineDao()
    val routineActionDao: RoutineActionDao = db.routineActionDao()
    
    suspend fun createRoutine(
        name: String,
        description: String?,
        triggerType: String,
        triggerValue: String,
        actions: List<ToolCall>
    ): Long {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val actionsJson = json.encodeToString(actions)
        
        val routineId = routineDao.upsert(RoutineEntity(
            name = name,
            description = description,
            triggerType = triggerType,
            triggerValue = triggerValue,
            actionsJson = actionsJson
        ))
        
        // Insert actions
        val actionEntities = actions.mapIndexed { index, call ->
            RoutineActionEntity(
                routineId = routineId,
                toolName = call.tool,
                argsJson = json.encodeToString(call.args),
                orderIndex = index
            )
        }
        routineActionDao.insertAll(actionEntities)
        
        return routineId
    }
    
    suspend fun getRoutine(name: String): RoutineEntity? = routineDao.getByName(name)
    
    val enabledRoutines: Flow<List<RoutineEntity>> = routineDao.getEnabled()
    val allRoutines: Flow<List<RoutineEntity>> = routineDao.getAll()
    
    val routinesByTrigger: (String) -> Flow<List<RoutineEntity>> = { type ->
        routineDao.getByTriggerType(type)
    }
    
    suspend fun runRoutine(name: String): List<Pair<String, Any>> {
        val routine = routineDao.getByName(name) ?: return emptyList()
        val actions = routineActionDao.getByRoutine(routine.id!!).first()
        
        routineDao.updateLastRun(routine.id!!, System.currentTimeMillis())
        
        return actions.map { action ->
            action.toolName to kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString<Map<String, Any>>(action.argsJson)
        }
    }
    
    suspend fun deleteRoutine(name: String) {
        routineDao.getByName(name)?.let { routine ->
            routineActionDao.deleteByRoutine(routine.id!!)
            routineDao.delete(routine)
        }
    }
    
    // "Pixel, aprende esto" - grabar secuencia
    private var recordingActions = mutableListOf<ToolCall>()
    private var isRecording = false
    
    fun startRecording() {
        isRecording = true
        recordingActions.clear()
    }
    
    fun recordAction(tool: String, args: Map<String, Any>) {
        if (isRecording) {
            recordingActions.add(ToolCall(tool, args))
        }
    }
    
    fun stopRecording(): List<ToolCall> {
        isRecording = false
        return recordingActions.toList()
    }
    
    fun saveRecordingAsRoutine(name: String, description: String?, triggerType: String = "voice", triggerValue: String = name): Long? {
        val actions = stopRecording()
        if (actions.isEmpty()) return null
        return createRoutine(name, description, triggerType, triggerValue, actions)
    }
    
    fun isRecording(): Boolean = isRecording
    
    fun getRecordedActions(): List<ToolCall> = recordingActions.toList()
}