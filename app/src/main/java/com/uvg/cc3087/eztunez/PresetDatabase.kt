package com.uvg.cc3087.eztunez

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "custom_presets", indices = [Index(value = ["nameKey"], unique = true)])
data class CustomPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val nameKey: String,
    val description: String,
    val string1: Int,
    val string2: Int,
    val string3: Int,
    val string4: Int,
    val string5: Int,
    val string6: Int
) {
    fun toPreset() = TuningPreset(
        // Built-in IDs are positive. Custom IDs are negative and cannot collide.
        id = -id, name = name, description = description,
        midiNotes = listOf(string1, string2, string3, string4, string5, string6),
        category = PresetCategory.CUSTOM
    )
}

@Dao
interface CustomPresetDao {
    @Query("SELECT * FROM custom_presets ORDER BY nameKey, id")
    fun observeAll(): Flow<List<CustomPresetEntity>>

    @Query("SELECT * FROM custom_presets WHERE id = :id")
    suspend fun find(id: Int): CustomPresetEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM custom_presets WHERE nameKey = :key AND id != :excluding)")
    suspend fun nameExists(key: String, excluding: Int): Boolean

    @Insert suspend fun insert(preset: CustomPresetEntity): Long
    @Update suspend fun update(preset: CustomPresetEntity): Int
    @Query("DELETE FROM custom_presets WHERE id = :id") suspend fun delete(id: Int): Int
}

@Database(entities = [CustomPresetEntity::class], version = 1, exportSchema = true)
abstract class PresetDatabase : RoomDatabase() {
    abstract fun customPresets(): CustomPresetDao

    companion object {
        @Volatile private var instance: PresetDatabase? = null
        fun getInstance(context: Context): PresetDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, PresetDatabase::class.java, "tuning_presets.db"
            ).build().also { instance = it }
        }
    }
}
