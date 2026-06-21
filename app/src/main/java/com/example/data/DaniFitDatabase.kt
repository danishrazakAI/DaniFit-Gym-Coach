package com.example.data

import android.content.Context
import androidx.room.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

// ========================
// 1. DATA ENTITIES
// ========================

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val age: Int,
    val gender: String,
    val height: Float, // cm
    val weight: Float, // kg
    val bodyFat: Float?, // %
    val fitnessLevel: String, // Beginner, Intermediate, Advanced
    val fitnessGoal: String, // Weight Loss, Fat Loss, Muscle Gain, etc.
    val medicalConditions: String,
    val lifestyleActivity: String, // Sedentary, Lightly Active, Moderately Active, Very Active
    val gymAccess: String, // Gym & Home, Home Workout Only
    val availableEquipment: String,
    val workoutDaysPerWeek: Int,
    val onboarded: Boolean = true
) {
    // Assessment Helpers
    val bmi: Float get() = if (height > 0) weight / ((height / 100f) * (height / 100f)) else 0f
    
    val bmr: Float get() {
        // Harris-Benedict formulas
        return if (gender.lowercase().startsWith("m")) {
            88.362f + (13.397f * weight) + (4.799f * height) - (5.677f * age)
        } else {
            447.593f + (9.247f * weight) + (3.098f * height) - (4.330f * age)
        }
    }

    val dailyCalorieNeeds: Float get() {
        val multiplier = when (lifestyleActivity) {
            "Sedentary" -> 1.2f
            "Lightly Active" -> 1.375f
            "Moderately Active" -> 1.55f
            "Very Active" -> 1.725f
            else -> 1.2f
        }
        val baseCal = bmr * multiplier
        return when (fitnessGoal) {
            "Weight Loss", "Fat Loss" -> baseCal - 500f
            "Muscle Gain", "Strength Building" -> baseCal + 300f
            else -> baseCal
        }
    }

    val macroProtein: Float get() {
        // 2g per kg for muscle gain/recomp, 1.6g for loss/others
        val factor = if (fitnessGoal.lowercase().contains("muscle") || fitnessGoal.lowercase().contains("strength")) 2.0f else 1.6f
        return weight * factor
    }

    val macroFats: Float get() {
        // 20-30% of calorie needs
        return (dailyCalorieNeeds * 0.25f) / 9f
    }

    val macroCarbs: Float get() {
        // Remaining calories
        val protCals = macroProtein * 4f
        val fatCals = macroFats * 9f
        val remCals = dailyCalorieNeeds - protCals - fatCals
        return if (remCals > 0) remCals / 4f else 0f
    }

    val waterIntakeRequirement: Float get() {
        // 35ml per kg base + extra for active lifestyles
        val base = weight * 0.035f
        return if (lifestyleActivity == "Very Active") base + 1.0f else base + 0.5f
    }
}

@Entity(tableName = "step_logs")
data class StepLog(
    @PrimaryKey val date: String, // "yyyy-MM-dd"
    val stepsCount: Int,
    val targetSteps: Int = 10000,
    val activeMinutes: Int = 0,
    val distanceKm: Float = 0f,
    val caloriesBurned: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_history")
data class WorkoutHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // "yyyy-MM-dd"
    val workoutName: String,
    val caloriesBurned: Int,
    val durationMinutes: Int,
    val exercisesCompleted: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user", "ai", "system"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "integrations")
data class IntegrationStatus(
    @PrimaryKey val serviceId: String, // "youtube", "google_fit", "apple_health", "fitbit", "garmin"
    val name: String,
    val isConnected: Boolean,
    val lastSyncTimestamp: Long = 0,
    val syncMessage: String = "Not connected",
    val apiToken: String? = null
)

// ========================
// 2. MODEL HELPER DATA CLASSES FOR JSON SERIALIZATION
// ========================

data class Exercise(
    val name: String,
    val sets: Int,
    val reps: String, // e.g., "10-12" or "12" or "As many as possible"
    val restTimeSeconds: Int,
    val targetMuscle: String,
    val caloriesBurned: Int,
    val difficulty: String, // Beginner, Intermediate, Advanced
    val formInstructions: String,
    val videoThumbnail: String? = null,
    val videoDuration: String = "2 min",
    val equipmentRequired: String = "Bodyweight",
    val altExercise: String? = null,
    val beginnerVariation: String? = null,
    val advancedVariation: String? = null
)

data class WorkoutPlan(
    val id: Int = 0,
    val planType: String, // "Daily", "Weekly", "90-Day"
    val dayLabel: String, // e.g. "Day 1" or "Monday"
    val workoutName: String,
    val targetMuscleGroups: String,
    val difficulty: String,
    val exercises: List<Exercise>
)

data class Meal(
    val name: String,
    val calories: Int,
    val protein: Float, // grams
    val carbs: Float, // grams
    val fats: Float, // grams
    val ingredients: List<String>,
    val preparationSteps: List<String>
)

data class DietPlan(
    val id: Int = 0,
    val planName: String, // "Standard", "Keto", "High Protein", etc.
    val breakfast: Meal,
    val lunch: Meal,
    val dinner: Meal,
    val snack: Meal
)

// ========================
// 3. DAOS
// ========================

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Query("DELETE FROM user_profile")
    suspend fun clearProfile()
}

@Dao
interface StepLogDao {
    @Query("SELECT * FROM step_logs ORDER BY date DESC")
    fun getAllLogsFlow(): Flow<List<StepLog>>

    @Query("SELECT * FROM step_logs WHERE date = :date LIMIT 1")
    suspend fun getLogForDate(date: String): StepLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: StepLog)

    @Query("SELECT SUM(stepsCount) FROM step_logs")
    suspend fun getTotalSteps(): Int?
}

@Dao
interface WorkoutHistoryDao {
    @Query("SELECT * FROM workout_history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<WorkoutHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WorkoutHistory)

    @Query("SELECT SUM(caloriesBurned) FROM workout_history")
    suspend fun getTotalCaloriesBurned(): Int?

    @Query("SELECT COUNT(*) FROM workout_history")
    suspend fun getTotalWorkoutsCompleted(): Int?
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()
}

@Dao
interface IntegrationStatusDao {
    @Query("SELECT * FROM integrations")
    fun getAllIntegrationsFlow(): Flow<List<IntegrationStatus>>

    @Query("SELECT * FROM integrations WHERE serviceId = :id LIMIT 1")
    suspend fun getIntegration(id: String): IntegrationStatus?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntegration(integration: IntegrationStatus)

    @Query("UPDATE integrations SET isConnected = :connected, syncMessage = :msg, lastSyncTimestamp = :ts WHERE serviceId = :id")
    suspend fun updateConnection(id: String, connected: Boolean, msg: String, ts: Long)

    @Query("DELETE FROM integrations")
    suspend fun clearIntegrations()
}

// ========================
// 4. TYPE CONVERTERS
// ========================

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, java.lang.String::class.java)
        return moshi.adapter<List<String>>(type).toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, java.lang.String::class.java)
        return moshi.adapter<List<String>>(type).fromJson(value)
    }
}

// ========================
// 5. ROOM DATABASE ARCHITECTURE
// ========================

@Database(
    entities = [UserProfile::class, StepLog::class, WorkoutHistory::class, ChatMessage::class, IntegrationStatus::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DaniFitDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun stepLogDao(): StepLogDao
    abstract fun workoutHistoryDao(): WorkoutHistoryDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun integrationStatusDao(): IntegrationStatusDao

    companion object {
        @Volatile
        private var INSTANCE: DaniFitDatabase? = null

        fun getDatabase(context: Context): DaniFitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DaniFitDatabase::class.java,
                    "danifit_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
