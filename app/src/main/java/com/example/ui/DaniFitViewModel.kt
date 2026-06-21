package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DaniFitViewModel(application: Application) : AndroidViewModel(application) {
    
    val database = DaniFitDatabase.getDatabase(application)
    val geminiService = GeminiService()
    val repository = DaniFitRepository(database, geminiService)

    // UI States
    val userProfile: StateFlow<UserProfile?> = repository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val stepLogs: StateFlow<List<StepLog>> = repository.allStepLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutHistory: StateFlow<List<WorkoutHistory>> = repository.allWorkoutHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = repository.allMessagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val integrations: StateFlow<List<IntegrationStatus>> = repository.allIntegrationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Planning States
    private val _currentWorkoutPlan = MutableStateFlow<WorkoutPlan?>(null)
    val currentWorkoutPlan: StateFlow<WorkoutPlan?> = _currentWorkoutPlan.asStateFlow()

    private val _currentDietPlan = MutableStateFlow<DietPlan?>(null)
    val currentDietPlan: StateFlow<DietPlan?> = _currentDietPlan.asStateFlow()

    // Push/Pull Session Tracking & Customizable Levels States
    private val _selectedWorkoutSplit = MutableStateFlow("Push")
    val selectedWorkoutSplit: StateFlow<String> = _selectedWorkoutSplit.asStateFlow()

    private val _selectedFitnessLevelOverride = MutableStateFlow<String?>(null)
    val selectedFitnessLevelOverride: StateFlow<String?> = _selectedFitnessLevelOverride.asStateFlow()

    private val _isWorkoutSessionActive = MutableStateFlow(false)
    val isWorkoutSessionActive: StateFlow<Boolean> = _isWorkoutSessionActive.asStateFlow()

    private val _workoutSessionElapsedSeconds = MutableStateFlow(0L)
    val workoutSessionElapsedSeconds: StateFlow<Long> = _workoutSessionElapsedSeconds.asStateFlow()

    private val _workoutSessionExercises = MutableStateFlow<List<ExerciseSessionState>>(emptyList())
    val workoutSessionExercises: StateFlow<List<ExerciseSessionState>> = _workoutSessionExercises.asStateFlow()

    private val _activeSessionSummary = MutableStateFlow<SessionSummary?>(null)
    val activeSessionSummary: StateFlow<SessionSummary?> = _activeSessionSummary.asStateFlow()

    private var sessionTimerJob: kotlinx.coroutines.Job? = null
    private var sessionStartTimeMillis = 0L

    fun setWorkoutSplit(split: String) {
        _selectedWorkoutSplit.value = split
    }

    fun setFitnessLevelOverride(level: String) {
        _selectedFitnessLevelOverride.value = level
    }

    fun startWorkoutSession() {
        val profileLevel = userProfile.value?.fitnessLevel ?: "Beginner"
        val level = _selectedFitnessLevelOverride.value ?: profileLevel
        val split = _selectedWorkoutSplit.value
        val baseExercises = getPushPullExercises(split, level)
        
        val initialSessionExercises = baseExercises.map { exe ->
            val defaultWeight = when {
                exe.name.lowercase().contains("deadlift") -> 50f
                exe.name.lowercase().contains("bench press") -> 40f
                exe.name.lowercase().contains("shoulder press") || exe.name.lowercase().contains("overhead press") -> 20f
                exe.name.lowercase().contains("curls") || exe.name.lowercase().contains("raises") -> 10f
                exe.name.lowercase().contains("rows") -> 30f
                else -> 0f
            }
            ExerciseSessionState(
                exerciseName = exe.name,
                defaultReps = exe.reps,
                defaultWeightKg = defaultWeight,
                restTimeSeconds = exe.restTimeSeconds,
                targetMuscle = exe.targetMuscle,
                caloriesBurned = exe.caloriesBurned,
                repsLogged = exe.reps,
                weightLoggedKg = defaultWeight.toInt().toString(),
                isCompleted = false
            )
        }
        
        _workoutSessionExercises.value = initialSessionExercises
        _isWorkoutSessionActive.value = true
        _workoutSessionElapsedSeconds.value = 0L
        sessionStartTimeMillis = System.currentTimeMillis()
        
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            while (_isWorkoutSessionActive.value) {
                kotlinx.coroutines.delay(1000)
                _workoutSessionElapsedSeconds.value = (System.currentTimeMillis() - sessionStartTimeMillis) / 1000
            }
        }
    }

    fun updateSessionExerciseLogged(name: String, reps: String, weight: String) {
        val currentList = _workoutSessionExercises.value.toMutableList()
        val index = currentList.indexOfFirst { it.exerciseName == name }
        if (index != -1) {
            currentList[index] = currentList[index].copy(
                repsLogged = reps,
                weightLoggedKg = weight
            )
            _workoutSessionExercises.value = currentList
        }
    }

    fun startRestTimer(exerciseName: String) {
        val currentList = _workoutSessionExercises.value.toMutableList()
        val index = currentList.indexOfFirst { it.exerciseName == exerciseName }
        if (index == -1) return
        
        val item = currentList[index]
        if (item.isTimerRunning) return
        
        currentList[index] = item.copy(
            isTimerRunning = true,
            restSecondsRemaining = item.restTimeSeconds
        )
        _workoutSessionExercises.value = currentList
        
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val activeList = _workoutSessionExercises.value.toMutableList()
                val idx = activeList.indexOfFirst { idxItem -> idxItem.exerciseName == exerciseName }
                if (idx == -1) break
                val actItem = activeList[idx]
                if (!actItem.isTimerRunning) break
                
                val remSeconds = actItem.restSecondsRemaining - 1
                if (remSeconds <= 0) {
                    activeList[idx] = actItem.copy(
                        isTimerRunning = false,
                        restSecondsRemaining = 0
                    )
                    _workoutSessionExercises.value = activeList
                    break
                } else {
                    activeList[idx] = actItem.copy(
                        restSecondsRemaining = remSeconds
                    )
                    _workoutSessionExercises.value = activeList
                }
            }
        }
    }

    fun toggleExerciseCompleted(name: String) {
        val currentList = _workoutSessionExercises.value.toMutableList()
        val index = currentList.indexOfFirst { it.exerciseName == name }
        if (index != -1) {
            val item = currentList[index]
            val nextCompleted = !item.isCompleted
            currentList[index] = item.copy(
                isCompleted = nextCompleted,
                isTimerRunning = false,
                restSecondsRemaining = 0
            )
            _workoutSessionExercises.value = currentList
            
            // Auto start rest countdown if marked completed
            if (nextCompleted && item.restTimeSeconds > 0) {
                startRestTimer(name)
            }
        }
    }

    fun finishWorkoutSession(feeling: String) {
        viewModelScope.launch {
            val elapsedSecs = _workoutSessionElapsedSeconds.value
            val minutes = (elapsedSecs / 60).toInt()
            val seconds = (elapsedSecs % 60).toInt()
            val split = _selectedWorkoutSplit.value
            
            val exercisesList = _workoutSessionExercises.value
            val completedCount = exercisesList.count { it.isCompleted }
            val totalCalories = exercisesList.filter { it.isCompleted }.sumOf { it.caloriesBurned }
            
            val context = getApplication<Application>()
            val sharedPrefs = context.getSharedPreferences("danifit_prs", Context.MODE_PRIVATE)
            val prsList = mutableListOf<PersonalRecordEarned>()
            
            exercisesList.forEach { item ->
                if (item.isCompleted) {
                    val weightInput = item.weightLoggedKg.toFloatOrNull() ?: 0f
                    if (weightInput > 0f) {
                        val prevPR = sharedPrefs.getFloat("PR_${item.exerciseName}", 0f)
                        if (weightInput > prevPR) {
                            sharedPrefs.edit().putFloat("PR_${item.exerciseName}", weightInput).apply()
                            prsList.add(PersonalRecordEarned(
                                exerciseName = item.exerciseName,
                                newRecordKg = weightInput,
                                previousRecordKg = prevPR
                            ))
                        }
                    }
                }
            }
            
            val summary = SessionSummary(
                splitType = split,
                timeMinutes = minutes,
                timeSeconds = seconds,
                caloriesBurned = totalCalories,
                exercisesCompleted = completedCount,
                prsEarned = prsList,
                feelingEmojiSelected = feeling
            )
            
            _activeSessionSummary.value = summary
            
            val workoutLabel = "$split Split ($feeling)"
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val todayStr = formatter.format(java.util.Date())
            repository.logCompletedWorkout(
                WorkoutHistory(
                    date = todayStr,
                    workoutName = workoutLabel,
                    caloriesBurned = totalCalories,
                    durationMinutes = if (minutes == 0) 1 else minutes,
                    exercisesCompleted = completedCount
                )
            )
            
            val prText = if (prsList.isNotEmpty()) {
                "\n\n🔥 **New Personal Records!**\n" + prsList.joinToString("\n") { "• ${it.exerciseName}: ${it.newRecordKg} Kg (was ${it.previousRecordKg} Kg)" }
            } else ""
            repository.insertMessage(ChatMessage(
                sender = "system",
                message = "🏆 **Workout Logged!** You finished a **$split Split** workout in **$minutes min $seconds sec**, burning **$totalCalories Calories**! You rated this session: **$feeling**.$prText"
            ))
            
            _isWorkoutSessionActive.value = false
            sessionTimerJob?.cancel()
            sessionTimerJob = null
            _workoutSessionElapsedSeconds.value = 0L
            _workoutSessionExercises.value = emptyList()
            
            refreshAggregates()
        }
    }

    fun dismissSessionSummary() {
        _activeSessionSummary.value = null
    }

    fun getPushPullExercises(split: String, level: String): List<Exercise> {
        return if (split == "Push") {
            when (level) {
                "Beginner" -> listOf(
                    Exercise("Knee Push-Ups", 3, "12", 45, "Chest & Triceps", 80, "Beginner", "Keep hands shoulder-width, lower chest slowly, push back up with chest squeeze.", "ic_danifit_logo", "1m 45s", "None"),
                    Exercise("Seated Dumbbell Shoulder Press", 3, "10", 60, "Shoulders", 90, "Beginner", "Press dumbbells straight up overhead from shoulder height, avoiding arching your back.", "ic_danifit_logo", "2m 10s", "Dumbbells"),
                    Exercise("Tricep Bench Dips", 3, "12", 60, "Triceps", 70, "Beginner", "Rest hands on bench behind you, lower hips until elbows are at 90 degrees, push up.", "ic_danifit_logo", "1m 30s", "Chair/Bench"),
                    Exercise("Standing Dumbbell Lateral Raises", 3, "12", 45, "Shoulders", 60, "Beginner", "Raise weights out to sides with slight elbow bend, control down.", "ic_danifit_logo", "1m 15s", "Dumbbells")
                )
                "Intermediate" -> listOf(
                    Exercise("Flat Barbell Bench Press", 4, "10", 90, "Chest & Triceps", 140, "Intermediate", "Lower the bar slowly to mid-chest, drive bar upwards vertically with full control.", "ic_danifit_logo", "3m 00s", "Barbell / Bench"),
                    Exercise("Incline Dumbbell Press", 3, "12", 75, "Upper Chest & Delts", 120, "Intermediate", "Set incline to 30 degrees, press dumbbells overhead, lower under control.", "ic_danifit_logo", "2m 20s", "Dumbbells"),
                    Exercise("Standing Overhead Barbell Press", 3, "10", 90, "Shoulders & Core", 110, "Intermediate", "Bar loaded on shoulders, press bar vertically overhead, locking out at top.", "ic_danifit_logo", "2m 50s", "Barbell & Rack"),
                    Exercise("Overhead Dumbbell Tricep Extension", 3, "12", 60, "Triceps", 80, "Intermediate", "Hold dumbbell with both hands behind head, extend elbows upward.", "ic_danifit_logo", "1m 40s", "Dumbbells"),
                    Exercise("Dumbbell Lateral Raises", 3, "12", 60, "Shoulders", 70, "Intermediate", "Raise dumbbells out to side in flat plane, squeeze rear shoulder blades.", "ic_danifit_logo", "1m 50s", "Dumbbells")
                )
                else -> listOf(
                    Exercise("Weighted Chest Dips", 4, "8", 90, "Lower Chest & Triceps", 160, "Advanced", "Attach plate to belt, dip deep below 90 degree elbow bend, drive back up.", "ic_danifit_logo", "2m 45s", "Dip Belt / Parallel Bars"),
                    Exercise("Heavy Barbell Bench Press", 4, "6", 120, "Pectoralis Major", 180, "Advanced", "Unrack heavy bar, lower under tension, push with explosive chest force.", "ic_danifit_logo", "3m 20s", "Barbell / Bench"),
                    Exercise("Handstand Push-Ups", 3, "8", 90, "Shoulders & Core", 130, "Advanced", "With back against wall or freestanding, bend elbows, kiss head to ground, drive up.", "ic_danifit_logo", "2m 00s", "Wall / None"),
                    Exercise("Cable Pec Flyes", 3, "15", 60, "Inner Chest", 90, "Advanced", "With cable handles at medium height, fly hands together in front and chest squeeze.", "ic_danifit_logo", "2m 15s", "Cable Machine"),
                    Exercise("Heavy Dumbbell Lateral Raises", 4, "15", 60, "Deltoids", 80, "Advanced", "Explosive raise of heavy dumbbells to side with slow, strict eccentric drop.", "ic_danifit_logo", "1m 50s", "Dumbbells"),
                    Exercise("Barbell Skull Crushers", 3, "10", 75, "Triceps (Long Head)", 100, "Advanced", "Lie on bench, lower EZ-bar to forehead keeping elbows completely vertical.", "ic_danifit_logo", "2m 30s", "EZ Bar / Bench")
                )
            }
        } else {
            when (level) {
                "Beginner" -> listOf(
                    Exercise("Lat Pulldown (Cable)", 3, "12", 60, "Lats & Upper Back", 90, "Beginner", "Sit upright, pull bar to collarbone using back, keep elbows aligned.", "ic_danifit_logo", "2m 10s", "Cable Machine"),
                    Exercise("Single-Arm Dumbbell Rows", 3, "12", 60, "Rhomboids & Traps", 80, "Beginner", "One hand and knee on bench, pull dumbbell to hip keeping back perfectly flat.", "ic_danifit_logo", "1m 45s", "Dumbbells"),
                    Exercise("Standing Dumbbell Bicep Curls", 3, "12", 60, "Biceps", 70, "Beginner", "Hold weights, curl up with chest open and palms rotating up.", "ic_danifit_logo", "1m 15s", "Dumbbells"),
                    Exercise("Resistance Band Pull-Aparts", 3, "15", 45, "Rear Delts", 50, "Beginner", "Hold band out front at shoulder level, pull hands outward stretching band across chest.", "ic_danifit_logo", "1m 00s", "Resistance Band")
                )
                "Intermediate" -> listOf(
                    Exercise("Standard Pull-Ups", 3, "8", 90, "Lats & Biceps", 120, "Intermediate", "Hang from bar, pull chest to bar, lower slowly with fully extended arms.", "ic_danifit_logo", "2m 30s", "Pull-up Bar"),
                    Exercise("Barbell Bent-Over Rows", 4, "10", 90, "Mid Back & Core", 130, "Intermediate", "Hinge at hips, pull bar to lower chest squeezing shoulder blades together.", "ic_danifit_logo", "2m 45s", "Barbell & Plates"),
                    Exercise("Seated Row (Cable)", 3, "12", 75, "Mid Back & Rhomboids", 100, "Intermediate", "Grab D-handle, pull to upper abdomen, stretch back out with deep lat extend.", "ic_danifit_logo", "2m 00s", "Cable Machine"),
                    Exercise("Incline Dumbbell Bicep Curls", 3, "10", 60, "Biceps (Long Head)", 80, "Intermediate", "Sit on 45 deg incline bench, let arms hang, curl dumbbells keeping elbows back.", "ic_danifit_logo", "1m 45s", "Dumbbells"),
                    Exercise("Rear Delt Dumbbell Flyes", 3, "12", 60, "Rear Delts", 70, "Intermediate", "Bent forward at 45 deg, swing weights out to side squeezing rear delts.", "ic_danifit_logo", "1m 30s", "Dumbbells")
                )
                else -> listOf(
                    Exercise("Weighted Pull-Ups", 4, "8", 120, "Lats & Arm Flexors", 160, "Advanced", "Strap weight to belt, pull chin clear over bar with ultra-strict control.", "ic_danifit_logo", "3m 00s", "Pull-up Bar / Weight Belt"),
                    Exercise("Barbell Deadlift", 3, "5", 120, "Posterior Chain", 210, "Advanced", "Double overhand grip, back dead-flat, lift off floor by driving hips forward.", "ic_danifit_logo", "4m 15s", "Barbell & Plates"),
                    Exercise("Heavy One-Arm Dumbbell Row", 4, "10", 90, "Latissimus Dorsi", 140, "Advanced", "Brace your leg, pull very heavy dumbbell to hip pocket with high-lat drive.", "ic_danifit_logo", "2m 00s", "Heavy Dumbbells"),
                    Exercise("Heavy Barbell Bicep Curl", 3, "8", 90, "Biceps Brachii", 90, "Advanced", "Strict trunk upright, barbell biceps squeeze with absolute minimal swing.", "ic_danifit_logo", "2m 10s", "Barbell"),
                    Exercise("Face Pulls (Cable)", 4, "15", 60, "Rear Deltoid & Rotators", 80, "Advanced", "Pulley at eye level, pull to forehead, opening arms outward in double bicep pose.", "ic_danifit_logo", "1m 50s", "Cable Machine"),
                    Exercise("Hammer Curls", 3, "12", 75, "Brachioradialis & Forearms", 80, "Advanced", "Curl dumbbell with neutral thumbs-up orientation to target forearm thickness.", "ic_danifit_logo", "1m 30s", "Dumbbells")
                )
            }
        }
    }

    // Loading states
    private val _isGeneratingPlan = MutableStateFlow(false)
    val isGeneratingPlan: StateFlow<Boolean> = _isGeneratingPlan.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Chat active input
    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    // Daily Steps Stats Live (Today)
    val todayStepLog: StateFlow<StepLog?> = stepLogs.map { logs ->
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        logs.find { it.date == todayStr } ?: StepLog(todayStr, 0, 10000)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Aggregate Stats
    val totalCaloriesBurned = MutableStateFlow(0)
    val totalWorkoutsCompleted = MutableStateFlow(0)
    val totalStepsWalked = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            repository.verifyAndSeedDatabase()
            refreshAggregates()
        }
    }

    fun updateChatInput(text: String) {
        _chatInput.value = text
    }

    private suspend fun refreshAggregates() {
        totalCaloriesBurned.value = repository.getTotalCaloriesBurned()
        totalWorkoutsCompleted.value = repository.getTotalWorkoutsCompleted()
        totalStepsWalked.value = repository.getTotalStepsWalked()
    }

    // Complete Onboarding Profile Setup
    fun completeOnboarding(
        name: String, age: Int, gender: String, height: Float, weight: Float,
        bodyFat: Float?, fitnessLevel: String, fitnessGoal: String,
        medicalConditions: String, lifestyleActivity: String,
        gymAccess: String, availableEquipment: String, workoutDaysPerWeek: Int
    ) {
        viewModelScope.launch {
            val profile = UserProfile(
                name = name,
                age = age,
                gender = gender,
                height = height,
                weight = weight,
                bodyFat = bodyFat,
                fitnessLevel = fitnessLevel,
                fitnessGoal = fitnessGoal,
                medicalConditions = medicalConditions,
                lifestyleActivity = lifestyleActivity,
                gymAccess = gymAccess,
                availableEquipment = availableEquipment,
                workoutDaysPerWeek = workoutDaysPerWeek,
                onboarded = true
            )
            repository.saveProfile(profile)
            
            // Auto generate initial workout and diet plans
            generatePlans()
            
            // Greet the user via Chatbot
            repository.insertMessage(ChatMessage(
                sender = "ai",
                message = "Welcome to DaniFit Coach, $name! 🌟 I've completed your comprehensive AI assessment. I calculated your BMI at ${String.format("%.1f", profile.bmi)} and daily BMR needs at ${String.format("%.0f", profile.bmr)} calories.\n\nI have generated your dynamic Workout Routines and Nutritional Diet templates based on your target: **$fitnessGoal**. Let's crush this journey together!"
            ))
            refreshAggregates()
        }
    }

    // Trigger Regenerate Plans based on Profile
    fun generatePlans() {
        viewModelScope.launch {
            _isGeneratingPlan.value = true
            try {
                // Get active profile to build correct recommendations
                val profile = repository.getProfile()
                if (profile != null) {
                    val wp = repository.getWorkoutPlan("90-Day Transformation Plan")
                    _currentWorkoutPlan.value = wp
                    
                    val goalDietName = when {
                        profile.fitnessGoal.contains("Gain") -> "High Protein Diet"
                        profile.fitnessGoal.contains("Loss") -> "Low Carb Shred"
                        else -> "Standard Balanced"
                    }
                    val dp = repository.getDietPlan(goalDietName)
                    _currentDietPlan.value = dp
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isGeneratingPlan.value = false
            }
        }
    }

    // Perform real exercise video recommendation update
    fun loadAlternativeForExercise(exerciseName: String) {
        val currentPlan = _currentWorkoutPlan.value ?: return
        val updatedExercises = currentPlan.exercises.map {
            if (it.name == exerciseName && it.altExercise != null) {
                // Swap exercise and its properties
                it.copy(
                    name = it.altExercise,
                    altExercise = it.name,
                    beginnerVariation = it.beginnerVariation ?: "Standard Position",
                    advancedVariation = it.advancedVariation ?: "Explosive Compound"
                )
            } else it
        }
        _currentWorkoutPlan.value = currentPlan.copy(exercises = updatedExercises)
    }

    // Perform video adjustment for beginner variation
    fun setBeginnerVariation(exerciseName: String) {
        val currentPlan = _currentWorkoutPlan.value ?: return
        val updatedExercises = currentPlan.exercises.map {
            if (it.name == exerciseName && it.beginnerVariation != null) {
                it.copy(
                    name = it.beginnerVariation,
                    beginnerVariation = it.name,
                    difficulty = "Beginner"
                )
            } else it
        }
        _currentWorkoutPlan.value = currentPlan.copy(exercises = updatedExercises)
    }

    // Perform video adjustment for advanced variation
    fun setAdvancedVariation(exerciseName: String) {
        val currentPlan = _currentWorkoutPlan.value ?: return
        val updatedExercises = currentPlan.exercises.map {
            if (it.name == exerciseName && it.advancedVariation != null) {
                it.copy(
                    name = it.advancedVariation,
                    advancedVariation = it.name,
                    difficulty = "Advanced"
                )
            } else it
        }
        _currentWorkoutPlan.value = currentPlan.copy(exercises = updatedExercises)
    }

    // Add ingredients and get custom recipe suggestions from AI Coach
    fun directSuggestRecipe(ingredients: String) {
        viewModelScope.launch {
            _isChatLoading.value = true
            _chatInput.value = ""
            
            // Insert user query
            val queryText = "Suggest a healthy meal with nutritional breakdown using these ingredients: $ingredients"
            repository.insertMessage(ChatMessage(sender = "user", message = queryText))
            
            val aiResponse = repository.getCoachingResponse(queryText)
            repository.insertMessage(ChatMessage(sender = "ai", message = aiResponse))
            _isChatLoading.value = false
        }
    }

    // Step stimulation tracker
    fun simulateSteps(stepsToChange: Int) {
        viewModelScope.launch {
            repository.addSimulatedSteps(stepsToChange)
            refreshAggregates()
        }
    }

    // Direct User Chat Prompt Submission
    fun sendChatMessage() {
        val text = _chatInput.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _chatInput.value = ""
            _isChatLoading.value = true
            
            // Insert into local Room database
            repository.insertMessage(ChatMessage(sender = "user", message = text))
            
            // Generate Gemini API response and insert
            val resultMessage = repository.getCoachingResponse(text)
            repository.insertMessage(ChatMessage(sender = "ai", message = resultMessage))
            
            _isChatLoading.value = false
            refreshAggregates()
        }
    }

    // Clear history logs for the user chat
    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChatHistory()
            repository.insertMessage(ChatMessage(
                sender = "ai",
                message = "Chat history cleared! Feel free to ask me anything about workouts, diet guidelines, step tracking, nutrition macros, or custom exercise plans."
            ))
        }
    }

    // Perform workout routine completion
    fun logWorkoutComplete(workoutName: String, calories: Int, repsCount: Int) {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.logCompletedWorkout(WorkoutHistory(
                date = todayStr,
                workoutName = workoutName,
                caloriesBurned = calories,
                durationMinutes = 45,
                exercisesCompleted = repsCount
            ))
            
            // Pop rewards and greetings
            repository.insertMessage(ChatMessage(
                sender = "system",
                message = "🏆 Congratulations! You successfully recorded and logged **$workoutName**! Burned **$calories kcal** and crushed **$repsCount exercises**!"
            ))
            
            refreshAggregates()
        }
    }

    // Integrations Handlers
    private val youtubeBridge = YouTubeBridge()
    private val appleHealthBridge = AppleHealthBridge()
    private var localStepCounterBridge: LocalStepCounterBridge? = null

    fun connectYouTube() {
        viewModelScope.launch {
            val apiKey = com.example.BuildConfig.YOUTUBE_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_YOUTUBE_API_KEY") {
                repository.connectIntegration("youtube", true, "Connected in Sandbox mode (No API Key)")
            } else {
                repository.connectIntegration("youtube", true, "Successfully connected via secure API key")
            }
        }
    }

    fun syncGoogleFit(context: Context) {
        viewModelScope.launch {
            val googleFit = GoogleFitBridge(context)
            val permitted = googleFit.requestPermissionsAndSync()
            if (permitted) {
                val stepCount = googleFit.fetchLastStepCount()
                // Sync steps to our database log for today
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = formatter.format(Date())
                repository.insertStepLog(StepLog(todayStr, stepCount, 10000, stepCount / 100, stepCount * 0.00075f, (stepCount * 0.045f).toInt()))
                repository.connectIntegration("google_fit", true, "Synced $stepCount steps today via Health Connect")
                refreshAggregates()
            } else {
                repository.connectIntegration("google_fit", false, "Permissions denied by developer system")
            }
        }
    }

    fun syncAppleHealth(appleToken: String) {
        viewModelScope.launch {
            val response = appleHealthBridge.syncWithICloudAndHealthKit(appleToken)
            if (response != null) {
                // Sync steps
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = formatter.format(Date())
                repository.insertStepLog(StepLog(todayStr, response.steps, 10000, response.activeMinutes, response.steps * 0.00075f, response.calories))
                repository.connectIntegration("apple_health", true, "Synced ${response.steps} steps from iOS HealthKit")
                refreshAggregates()
            }
        }
    }

    fun connectLocalStepSensor() {
         viewModelScope.launch {
             localStepCounterBridge?.stopListening()
             val context = getApplication<Application>()
             val bridge = LocalStepCounterBridge(context) { stepsIncrementByPedometer ->
                 viewModelScope.launch {
                     repository.addSimulatedSteps(stepsIncrementByPedometer)
                     refreshAggregates()
                 }
             }
             localStepCounterBridge = bridge
             bridge.startListening()
             repository.connectIntegration("android_sensor", true, "Enabled. Counting steps natively using device hardware movement sensors.")
             refreshAggregates()
         }
    }

    fun playExerciseVideo(context: Context, exerciseName: String) {
        viewModelScope.launch {
            val videos = try {
                youtubeBridge.findExerciseVideos(exerciseName)
            } catch (e: Exception) {
                emptyList()
            }
            
            val videoUrl = if (videos.isNotEmpty()) {
                videos.first().second
            } else {
                "https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode("$exerciseName form guide tutorial", "UTF-8")
            }

            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(videoUrl)).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun disconnectIntegration(serviceId: String) {
        viewModelScope.launch {
            if (serviceId == "android_sensor") {
                localStepCounterBridge?.stopListening()
                localStepCounterBridge = null
            }
            repository.connectIntegration(serviceId, false, "Not connected")
            refreshAggregates()
        }
    }

    override fun onCleared() {
        super.onCleared()
        localStepCounterBridge?.stopListening()
        localStepCounterBridge = null
    }
}

data class ExerciseSessionState(
    val exerciseName: String,
    val defaultReps: String,
    val defaultWeightKg: Float,
    val restTimeSeconds: Int,
    val targetMuscle: String,
    val caloriesBurned: Int,
    val repsLogged: String,
    val weightLoggedKg: String,
    val isCompleted: Boolean = false,
    val restSecondsRemaining: Int = 0,
    val isTimerRunning: Boolean = false
)

data class SessionSummary(
    val splitType: String,
    val timeMinutes: Int,
    val timeSeconds: Int,
    val caloriesBurned: Int,
    val exercisesCompleted: Int,
    val prsEarned: List<PersonalRecordEarned>,
    val feelingEmojiSelected: String = ""
)

data class PersonalRecordEarned(
    val exerciseName: String,
    val newRecordKg: Float,
    val previousRecordKg: Float
)
