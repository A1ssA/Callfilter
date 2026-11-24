package com.callguard.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class FilterMode {
    WHITELIST,
    BLACKLIST,
    DISABLED
}

enum class ListType {
    WHITELIST,
    BLACKLIST
}

class CallGuardConverters {
    @TypeConverter
    fun fromFilterMode(mode: FilterMode): String = mode.name

    @TypeConverter
    fun toFilterMode(name: String): FilterMode = FilterMode.valueOf(name)

    @TypeConverter
    fun fromListType(type: ListType): String = type.name

    @TypeConverter
    fun toListType(name: String): ListType = ListType.valueOf(name)
}

@Entity(tableName = "phone_rules")
data class PhoneRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val displayName: String,
    val description: String = "",
    val listType: ListType,
    val isPriority: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_history")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val contactName: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val wasBlocked: Boolean,
    val reason: String,
    val activeMode: FilterMode
)

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val targetMode: FilterMode,
    val fallbackMode: FilterMode,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeekMask: Int,
    val isEnabled: Boolean = true
)

@Dao
interface CallGuardDao {
    @Query("SELECT * FROM phone_rules WHERE listType = :type ORDER BY displayName ASC")
    fun getRulesByType(type: ListType): Flow<List<PhoneRuleEntity>>

    @Query("SELECT * FROM phone_rules WHERE listType = :type")
    suspend fun getRulesByTypeSync(type: ListType): List<PhoneRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: PhoneRuleEntity)

    @Update
    suspend fun updateRule(rule: PhoneRuleEntity)

    @Delete
    suspend fun deleteRule(rule: PhoneRuleEntity)

    @Query("SELECT * FROM call_history ORDER BY timestamp DESC LIMIT 200")
    fun getCallHistory(): Flow<List<CallLogEntity>>

    @Insert
    suspend fun insertCallLog(log: CallLogEntity)

    @Query("DELETE FROM call_history")
    suspend fun clearCallHistory()

    @Query("SELECT COUNT(*) FROM call_history WHERE phoneNumber = :number AND timestamp > :sinceTimestamp")
    suspend fun countRecentCallsFromNumber(number: String, sinceTimestamp: Long): Int

    @Query("SELECT * FROM schedules ORDER BY startHour ASC, startMinute ASC")
    fun getAllSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE isEnabled = 1")
    suspend fun getEnabledSchedulesSync(): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)
}

@Database(
    entities = [PhoneRuleEntity::class, CallLogEntity::class, ScheduleEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(CallGuardConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callGuardDao(): CallGuardDao
}
