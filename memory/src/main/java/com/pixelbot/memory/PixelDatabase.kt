package com.pixelbot.memory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import com.pixelbot.memory.util.Converters

@Database(
    entities = [
        ProfileEntity::class,
        ContactEntity::class,
        FavoriteAppEntity::class,
        CommandHistoryEntity::class,
        RoutineEntity::class,
        RoutineActionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PixelDatabase : RoomDatabase() {
    
    abstract fun profileDao(): ProfileDao
    abstract fun contactDao(): ContactDao
    abstract fun favoriteAppDao(): FavoriteAppDao
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineActionDao(): RoutineActionDao
    abstract fun settingsDao(): SettingsDao
    
    companion object {
        @Volatile private var INSTANCE: PixelDatabase? = null
        
        fun getInstance(context: Context): PixelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PixelDatabase::class.java,
                    "pixel_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}