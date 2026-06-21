package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class DaniFitRepository(
    private val db: DaniFitDatabase,
    private val geminiService: GeminiService
) {
    private val profileDao = db.userProfileDao()
    private val stepDao = db.stepLogDao()
    private val historyDao = db.workoutHistoryDao()
    private val msgDao = db.chatMessageDao()
    private val integrationDao = db.integrationStatusDao()

    // Observables (Flows)
    val userProfileFlow: Flow<UserProfile?> = profileDao.getProfileFlow()
    val allStepLogsFlow: Flow<List<StepLog>> = stepDao.getAllLogsFlow()
    val allWorkoutHistoryFlow: Flow<List<WorkoutHistory>> = historyDao.getAllHistoryFlow()
    val allMessagesFlow: Flow<List<ChatMessage>> = msgDao.getAllMessagesFlow()
    val allIntegrationsFlow: Flow<List<IntegrationStatus>> = integrationDao.getAllIntegrationsFlow()

    suspend fun getProfile(): UserProfile? = profileDao.getProfile()

    suspend fun saveProfile(profile: UserProfile) {
        profileDao.insertProfile(profile)
    }

    suspend fun saveIntegrationStatus(integration: IntegrationStatus) {
        integrationDao.insertIntegration(integration)
    }

    suspend fun connectIntegration(serviceId: String, connected: Boolean, message: String) {
        integrationDao.updateConnection(serviceId, connected, message, System.currentTimeMillis())
    }

    suspend fun insertStepLog(log: StepLog) {
        stepDao.insertLog(log)
    }

    suspend fun logCompletedWorkout(history: WorkoutHistory) {
        historyDao.insertHistory(history)
    }

    suspend fun insertMessage(message: ChatMessage) {
        msgDao.insertMessage(message)
    }

    suspend fun clearChatHistory() {
        msgDao.clearChatHistory()
    }

    // High level metrics
    suspend fun getTotalCaloriesBurned(): Int {
        return historyDao.getTotalCaloriesBurned() ?: 0
    }

    suspend fun getTotalWorkoutsCompleted(): Int {
        return historyDao.getTotalWorkoutsCompleted() ?: 0
    }

    suspend fun getTotalStepsWalked(): Int {
        return stepDao.getTotalSteps() ?: 0
    }

    // AI Planning Access
    suspend fun getWorkoutPlan(type: String): WorkoutPlan {
        val profile = getProfile() ?: return getFallbackWorkoutPlan(type)
        return geminiService.generateWorkoutPlanOffline(profile, type)
    }

    suspend fun getDietPlan(dietType: String): DietPlan {
        val profile = getProfile() ?: return getFallbackDietPlan(dietType)
        return geminiService.generateDietPlanOffline(profile, dietType)
    }

    // Helper for chat questions passing along the user profile
    suspend fun getCoachingResponse(prompt: String): String {
        val profile = getProfile()
        val chatHistory = allMessagesFlow.firstOrNull() ?: emptyList()
        return geminiService.getChatResponse(prompt, chatHistory, profile)
    }

    // Incremental step simulation for development purposes
    suspend fun addSimulatedSteps(amount: Int) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = formatter.format(Date())
        val existing = stepDao.getLogForDate(dateString)
        
        val target = 10000
        val steps = (existing?.stepsCount ?: 0) + amount
        val distance = steps * 0.00075f // roughly 75cm per step
        val calories = (steps * 0.045f).toInt() // approx calories burned
        val activeMin = (steps / 100) // approx 100 steps per active minute

        val stepLog = StepLog(
            date = dateString,
            stepsCount = steps,
            targetSteps = target,
            activeMinutes = activeMin,
            distanceKm = distance,
            caloriesBurned = calories
        )
        stepDao.insertLog(stepLog)
    }

    // Setup dummy data on first launch to make the UX rich and colorful immediately
    suspend fun verifyAndSeedDatabase() {
        // Only seed if empty
        val currentProfile = getProfile()
        if (currentProfile == null) {
            // Do not force onboard, we will let user complete wizard.
            // But we will insert default step logs for yesterday and today so there are trend graphs populated!
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            
            // Today
            val todayString = formatter.format(cal.time)
            stepDao.insertLog(StepLog(todayString, 4200, 10000, 42, 3.1f, 189))
            
            // Yesterday
            cal.add(Calendar.DATE, -1)
            val yesterdayString = formatter.format(cal.time)
            stepDao.insertLog(StepLog(yesterdayString, 8500, 10000, 85, 6.3f, 382))
            
            // Day before yesterday
            cal.add(Calendar.DATE, -1)
            val dayBeforeString = formatter.format(cal.time)
            stepDao.insertLog(StepLog(dayBeforeString, 11200, 10000, 112, 8.4f, 504))

            // Three days ago
            cal.add(Calendar.DATE, -1)
            val threeDaysAgoString = formatter.format(cal.time)
            stepDao.insertLog(StepLog(threeDaysAgoString, 9100, 10000, 91, 6.8f, 409))
        }

        // Always ensure default integrations are seeded fresh
        integrationDao.clearIntegrations()
        integrationDao.insertIntegration(IntegrationStatus("android_sensor", "Android Built-in Pedometer", false, 0, "Count steps locally using your device\'s hardware motion sensors"))
        integrationDao.insertIntegration(IntegrationStatus("google_fit", "Google Fit / Health Connect", false, 0, "Sync device fitness records directly via Android framework"))
        integrationDao.insertIntegration(IntegrationStatus("apple_health", "Apple HealthKit Bridge", false, 0, "iCloud Personal Health federation"))
        integrationDao.insertIntegration(IntegrationStatus("youtube", "YouTube API", false, 0, "Find workout tutorial videos dynamically"))
    }

    private fun getFallbackWorkoutPlan(type: String): WorkoutPlan {
        return WorkoutPlan(
            planType = type,
            dayLabel = "Active Training",
            workoutName = "Elite Full Body Developer",
            targetMuscleGroups = "Full Body",
            difficulty = "Beginner",
            exercises = listOf(
                Exercise("Bodyweight Squats", 3, "15", 45, "Quads", 80, "Beginner", "Keep chest tall, push hips back"),
                Exercise("Push-Ups", 3, "12", 60, "Chest & Arms", 60, "Beginner", "Maintain a straight plank posture"),
                Exercise("Plank", 3, "30s", 45, "Core", 30, "Beginner", "Squeeze glutes and draw belly button to spine")
            )
        )
    }

    private fun getFallbackDietPlan(type: String): DietPlan {
        val meal = Meal("High-Protein Muscle Cereal", 450, 32f, 45f, 10f, listOf("Oats", "Whey Protein", "Berries"), listOf("Cook oats, stir in whey"))
        return DietPlan(0, type, meal, meal, meal, meal)
    }
}
