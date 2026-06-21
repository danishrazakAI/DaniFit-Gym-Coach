package com.example.data

import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

// ========================
// GEMINI REST API PAYLOAD BUILDERS
// ========================

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContent?)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") key: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }
}

class GeminiService {

    suspend fun getChatResponse(prompt: String, chatHistory: List<ChatMessage>, profile: UserProfile?): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiService", "API Key is empty or placeholder! Running offline dynamic fit coach.")
            return generateOfflineCoachResponse(prompt, profile)
        }

        val profileContext = if (profile != null) {
            "User Profile: ${profile.name}, Age: ${profile.age}, Gender: ${profile.gender}, " +
            "Weight: ${profile.weight}kg, Height: ${profile.height}cm, Goal: ${profile.fitnessGoal}, " +
            "Fitness Level: ${profile.fitnessLevel}, Activity: ${profile.lifestyleActivity}, Gym Access: ${profile.gymAccess}."
        } else {
            "No active user profile created yet."
        }

        val systemPrompt = "You are DaniFit Coach, a certified fitness coach, personal trainer, nutritionist, " +
                "and medical-aware health companion. Talk directly, warmly, and encourage the user with professional " +
                "athletic insights. Always factor in any injuries, allergies, or conditions. $profileContext Keep responses relatively summary-focused and practical (maximum 2-3 short, highly-readable paragraphs)."

        val historyContents = chatHistory.takeLast(10).map { msg ->
            GeminiContent(parts = listOf(GeminiPart(text = "${if (msg.sender == "user") "User" else "Coach"}: ${msg.message}")))
        }

        val currentContent = GeminiContent(parts = listOf(GeminiPart(text = prompt)))
        val allContents = historyContents + currentContent

        val request = GeminiRequest(
            contents = allContents,
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        return try {
            val response = RetrofitClient.api.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "I apologize, but I couldn't process information right now. Let me know what you need!"
        } catch (e: Exception) {
            Log.e("GeminiService", "Error contacting Gemini API", e)
            generateOfflineCoachResponse(prompt, profile)
        }
    }

    private fun generateOfflineCoachResponse(prompt: String, profile: UserProfile?): String {
        val lowercase = prompt.lowercase()
        val name = profile?.name ?: "Athlete"
        val goal = profile?.fitnessGoal ?: "General Fitness"

        return when {
            lowercase.contains("hello") || lowercase.contains("hi") || lowercase.contains("hey") -> {
                "Hello $name! I am DaniFit Coach, your AI health and training companion. I'm ready to design elite workout programs, meal structures, monitor your step trends, or help you master movement. What muscle group or hydration question are we tackling today?"
            }
            lowercase.contains("protein") || lowercase.contains("eat") || lowercase.contains("macro") || lowercase.contains("diet") -> {
                if (profile != null) {
                    val p = String.format("%.1f", profile.macroProtein)
                    val c = String.format("%.1f", profile.macroCarbs)
                    val f = String.format("%.1f", profile.macroFats)
                    val cal = String.format("%.0f", profile.dailyCalorieNeeds)
                    "For your goal **$goal**, your daily recommendation is **$cal calories**: **${p}g protein**, **${c}g carbohydrates**, and **${f}g healthy fats**. Be sure to space protein intake evenly across 3-4 meals to maximize muscle protein synthesis!"
                } else {
                    "Generally, active athletes should target 1.6 to 2.2 grams of protein per kilogram of bodyweight, keeping carbs high for intense sessions, and healthy fats at 20-30% of total caloric intake."
                }
            }
            lowercase.contains("workout") || lowercase.contains("exercise") || lowercase.contains("chest") || lowercase.contains("leg") || lowercase.contains("gym") -> {
                "To optimize for **$goal**, I suggest a structured Split (Push/Pull/Legs) focusing on progressive overload. Perform multi-joint compound lifts (like Bench Press or Squats) first, followed by high-intensity isolation sets. I have populated a custom workout plan on your Planning Tab—check it out!"
            }
            lowercase.contains("water") || lowercase.contains("drink") || lowercase.contains("hydrate") -> {
                val dailyWater = if (profile != null) String.format("%.1f", profile.waterIntakeRequirement) else "3.0"
                "Hydration is vital! Based on your output, your body needs roughly **$dailyWater liters** of water daily. Increase this by 500-1000ml during active training windows to balance sweat losses and preserve muscle stamina."
            }
            else -> {
                "Excellent question! To achieve your **$goal** goal, stay consistent with progressive overload in your exercises, track your daily steps (target 10k), hit your calorie/protein targets, and sleep 7-8 hours. Ask me anything more about exercises or nutrition plans!"
            }
        }
    }

    // Dynamic High-Fidelity Workout Planner based on profile
    fun generateWorkoutPlanOffline(profile: UserProfile, type: String): WorkoutPlan {
        val level = profile.fitnessLevel
        val isHome = profile.gymAccess.contains("Home")
        val isLoss = profile.fitnessGoal.contains("Loss") || profile.fitnessGoal.contains("Fat")
        
        val exercises = mutableListOf<Exercise>()

        if (isLoss) {
            if (isHome) {
                exercises.add(Exercise(
                    name = "Bodyweight Squats", sets = 4, reps = "20", restTimeSeconds = 45,
                    targetMuscle = "Quads & Glutes", caloriesBurned = 120, difficulty = level,
                    formInstructions = "Keep feet shoulder-width, back straight, lower until thighs are parallel to ground, push up through heels.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "2m 15s", equipmentRequired = "None",
                    altExercise = "Lunges", beginnerVariation = "Chair Squats", advancedVariation = "Jump Squats"
                ))
                exercises.add(Exercise(
                    name = "Incline Push-Ups", sets = 3, reps = "15", restTimeSeconds = 60,
                    targetMuscle = "Chest & Triceps", caloriesBurned = 90, difficulty = level,
                    formInstructions = "Place hands on bench or wall, keep body in flat line, lower chest, push back up with chest squeeze.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "1m 45s", equipmentRequired = "Chair/Bench",
                    altExercise = "Regular Push-Ups", beginnerVariation = "Knee Push-Ups", advancedVariation = "Decline Push-Ups"
                ))
                exercises.add(Exercise(
                    name = "Glute Bridges", sets = 3, reps = "15", restTimeSeconds = 45,
                    targetMuscle = "Glutes & Hamstrings", caloriesBurned = 80, difficulty = level,
                    formInstructions = "Lie on back with knees bent, feet flat, squeeze glutes to lift hips to ceiling, hold for 1s, lower slowly.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "2m 00s", equipmentRequired = "None",
                    altExercise = "Single Leg Bridge", beginnerVariation = "Standard Bridge", advancedVariation = "Weighted Bridge"
                ))
                exercises.add(Exercise(
                    name = "Plank Hold", sets = 3, reps = "45 sec", restTimeSeconds = 45,
                    targetMuscle = "Core Strength", caloriesBurned = 70, difficulty = level,
                    formInstructions = "Rest forearms on floor, keep shoulders stacked over elbows, maintain perfectly flat spine and squeeze abs.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "3m 10s", equipmentRequired = "None",
                    altExercise = "Side Plank", beginnerVariation = "Knee Plank", advancedVariation = "Plank Jacks"
                ))
                exercises.add(Exercise(
                    name = "Mountain Climbers", sets = 3, reps = "30 sec", restTimeSeconds = 30,
                    targetMuscle = "Cardio Boost", caloriesBurned = 140, difficulty = level,
                    formInstructions = "Push up position, pull knees rapidly into chest alternating legs, keep hips low, pump high speed.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "1m 30s", equipmentRequired = "None",
                    altExercise = "Jumping Jacks", beginnerVariation = "Slow Climbers", advancedVariation = "Crossbody Climbers"
                ))
            } else {
                exercises.add(Exercise(
                    name = "Barbell Squats", sets = 4, reps = "12", restTimeSeconds = 90,
                    targetMuscle = "Quads & Glutes", caloriesBurned = 180, difficulty = level,
                    formInstructions = "Rest bar on upper traps, descend back while pushing knees open, keep chest high, stand and squeeze.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "3m 00s", equipmentRequired = "Barbell / Rack",
                    altExercise = "Leg Press", beginnerVariation = "Goblet Squat", advancedVariation = "Front Squats"
                ))
                exercises.add(Exercise(
                    name = "Dumbbell Incline Bench Press", sets = 4, reps = "12", restTimeSeconds = 75,
                    targetMuscle = "Upper Chest & Delts", caloriesBurned = 130, difficulty = level,
                    formInstructions = "Set incline to 30 degrees, press dumbbells directly upward, control weights on way down.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "2m 20s", equipmentRequired = "Dumbbells",
                    altExercise = "Barbell Incline Press", beginnerVariation = "Flat DB Press", advancedVariation = "DB Pec Flyes"
                ))
                exercises.add(Exercise(
                    name = "Lat Pulldown (Cable)", sets = 3, reps = "12", restTimeSeconds = 60,
                    targetMuscle = "Lats & Mid-Back", caloriesBurned = 110, difficulty = level,
                    formInstructions = "Sit tall, pull bar down towards upper chest using elbows, lean back slightly, release bar with control.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "2m 10s", equipmentRequired = "Cable Machine",
                    altExercise = "Pull-Ups", beginnerVariation = "Assisted Lat Pull", advancedVariation = "Weighted Pull-Ups"
                ))
                exercises.add(Exercise(
                    name = "Face Pulls", sets = 3, reps = "15", restTimeSeconds = 60,
                    targetMuscle = "Rear Delts & Rotators", caloriesBurned = 70, difficulty = level,
                    formInstructions = "Rope pulley at eye level, pull rope back to face, flare elbows outward, squeeze shoulder blades together.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "1m 50s", equipmentRequired = "Cable Machine",
                    altExercise = "Rear Delt Flyes", beginnerVariation = "Band Pull-aparts", advancedVariation = "DB Rear Flyes"
                ))
                exercises.add(Exercise(
                    name = "Elliptical HIIT Intervals", sets = 1, reps = "10 min", restTimeSeconds = 0,
                    targetMuscle = "Cardiovascular", caloriesBurned = 160, difficulty = "Intermediate",
                    formInstructions = "Alt between 30s sprinting at maximum resistance/speed and 60s slow recovery jogging.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "4m 00s", equipmentRequired = "Elliptical Trainer",
                    altExercise = "Treadmill Sprint", beginnerVariation = "Steady State Walking", advancedVariation = "Rowing Intervals"
                ))
            }
        } else {
            // Muscle Gain or Strength Building
            if (isHome) {
                exercises.add(Exercise(
                    name = "Pike Pushups", sets = 3, reps = "10", restTimeSeconds = 75,
                    targetMuscle = "Front Delts & Triceps", caloriesBurned = 100, difficulty = level,
                    formInstructions = "Hips high in V position, lower top of head slowly towards hands, push back through shoulders.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "2m 00s", equipmentRequired = "None",
                    altExercise = "Overhead DB Press", beginnerVariation = "Decline Push-ups", advancedVariation = "Handstand Pushups"
                ))
                exercises.add(Exercise(
                    name = "Bulgarian Split Squats", sets = 4, reps = "12 each", restTimeSeconds = 90,
                    targetMuscle = "Quads & Glutes", caloriesBurned = 140, difficulty = "Intermediate",
                    formInstructions = "One foot flat on ground, one foot resting on chair behind, squat down until back knee is near floor.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "2m 30s", equipmentRequired = "Chair/Bench",
                    altExercise = "Reverse Lunges", beginnerVariation = "Standard Lunges", advancedVariation = "Weighted Split Squat"
                ))
                exercises.add(Exercise(
                    name = "Doorframe Pull-In Rows", sets = 4, reps = "15", restTimeSeconds = 60,
                    targetMuscle = "Rhomboids & Lats", caloriesBurned = 90, difficulty = level,
                    formInstructions = "Grip doorframe with both hands, lean back fully, pull your chest toward doorframe with back activation.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "1m 40s", equipmentRequired = "Doorframe or Towel",
                    altExercise = "Inverted Table Rows", beginnerVariation = "Assisted Rows", advancedVariation = "Single-arm Door Row"
                ))
                exercises.add(Exercise(
                    name = "Towel Bicep Curls", sets = 3, reps = "12-15", restTimeSeconds = 60,
                    targetMuscle = "Biceps Brachii", caloriesBurned = 60, difficulty = level,
                    formInstructions = "Step on towel with one foot, pull towel handles upward with arms, squeeze biceps aggressively.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "1m 20s", equipmentRequired = "Towel",
                    altExercise = "Resistance Band Curls", beginnerVariation = "Standard Towel Isometric", advancedVariation = "Backpack Weighted Curls"
                ))
                exercises.add(Exercise(
                    name = "Bicycle Crunches", sets = 3, reps = "25", restTimeSeconds = 45,
                    targetMuscle = "Rectus Abdominis & Obliques", caloriesBurned = 70, difficulty = level,
                    formInstructions = "Lie on back, touch hand to opposite knee while cycling legs, crunch upward squeezing lateral abs.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "2m 10s", equipmentRequired = "None",
                    altExercise = "Plank", beginnerVariation = "Standard Crunch", advancedVariation = "Hanging Leg Raises"
                ))
            } else {
                exercises.add(Exercise(
                    name = "Barbell Bench Press", sets = 4, reps = "8", restTimeSeconds = 90,
                    targetMuscle = "Pectoralis Major", caloriesBurned = 150, difficulty = level,
                    formInstructions = "Lie on bench, bar aligned over eyes, lower slowly to mid-chest, drive bar up with heels pushed into floor.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "3m 20s", equipmentRequired = "Barbell / Bench",
                    altExercise = "Dumbbell Press", beginnerVariation = "Chest Press Machine", advancedVariation = "Weighted Dips"
                ))
                exercises.add(Exercise(
                    name = "Barbell Deadlift", sets = 3, reps = "6", restTimeSeconds = 120,
                    targetMuscle = "Hamstrings, Glutes & Spinal", caloriesBurned = 210, difficulty = "Advanced",
                    formInstructions = "Bar close to shins, flat back, grip bar, drive hips forward to lift bar, squeeze glutes at top.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "4m 15s", equipmentRequired = "Barbell & Plates",
                    altExercise = "Romanian Deadlift", beginnerVariation = "Kettlebell Deadlift", advancedVariation = "Deficit Deadlifts"
                ))
                exercises.add(Exercise(
                    name = "Overhead Barbell Press", sets = 4, reps = "8", restTimeSeconds = 90,
                    targetMuscle = "Anterior Deltoids", caloriesBurned = 120, difficulty = level,
                    formInstructions = "Bar loaded on shoulders, press bar straight up, clear your chin by tilting head back slightly.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "2m 50s", equipmentRequired = "Barbell & Rack",
                    altExercise = "DB Shoulder Press", beginnerVariation = "DB Overhead seated", advancedVariation = "Handstand Pushups"
                ))
                exercises.add(Exercise(
                    name = "Seated Neutral Row (Cable)", sets = 3, reps = "10", restTimeSeconds = 75,
                    targetMuscle = "Trapezius & Rhomboids", caloriesBurned = 100, difficulty = level,
                    formInstructions = "Feet on plates, grab double-D attachment, pull handles to belly button while opening shoulders.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "2m 00s", equipmentRequired = "Seated Row Cable",
                    altExercise = "DB Row", beginnerVariation = "Machine Row", advancedVariation = "Bent-over Barbell Rows"
                ))
                exercises.add(Exercise(
                    name = "Incline Dumbbell Curl", sets = 3, reps = "12", restTimeSeconds = 60,
                    targetMuscle = "Biceps (Long Head)", caloriesBurned = 60, difficulty = level,
                    formInstructions = "Sit back at 45 degree angle, let arms hang, curl weight keeping elbows pinned to back.",
                    videoThumbnail = "ic_danifit_logo", videoDuration = "1m 45s", equipmentRequired = "Dumbbells",
                    altExercise = "Preacher Curls", beginnerVariation = "Hammer Curls", advancedVariation = "Spider Curls"
                ))
            }
        }

        return WorkoutPlan(
            planType = type,
            dayLabel = "Active Day Routine",
            workoutName = if (isLoss) "Fat Shred & Cardio Burner" else "Elite Split & Hypertrophy Focus",
            targetMuscleGroups = if (isLoss) "Full Body & Cardio" else "General Strength Split",
            difficulty = level,
            exercises = exercises
        )
    }

    // Dynamic High-Fidelity Diet Planner based on profile
    fun generateDietPlanOffline(profile: UserProfile, dietTypeName: String): DietPlan {
        val cal = profile.dailyCalorieNeeds.toInt()
        val p = profile.macroProtein.toInt()
        val c = profile.macroCarbs.toInt()
        val f = profile.macroFats.toInt()

        val mealCal = cal / 4

        return DietPlan(
            planName = dietTypeName,
            breakfast = Meal(
                name = if (dietTypeName.lowercase().contains("keto")) "Avocado & Salmon Omelet" else "Axe High-Protein Berry Oats",
                calories = mealCal + 50,
                protein = (p * 0.25f),
                carbs = if (dietTypeName.lowercase().contains("keto")) 4f else (c * 0.30f),
                fats = if (dietTypeName.lowercase().contains("keto")) (f * 0.35f) else 10f,
                ingredients = listOf("3 Egg whites + 1 Whole egg", "Organic Rolled Oats (50g)", "Whey Protein scoop", "Mixed Frozen Berries"),
                preparationSteps = listOf("Cook oats in double weight water for 3 minutes.", "Stir in whey protein immediately until smooth.", "Top with chia seeds and frozen blueberries for antioxidants.")
            ),
            lunch = Meal(
                name = "DaniFit Grilled Lean Chicken Bowl",
                calories = mealCal + 100,
                protein = (p * 0.35f),
                carbs = if (dietTypeName.lowercase().contains("keto")) 6f else (c * 0.35f),
                fats = (f * 0.25f),
                ingredients = listOf("Grilled Chicken Breast (200g)", "White Basmati Rice (120g cooked)", "Steamed Broccoli & Carrot", "Extra Virgin Olive Oil (1 tsp)"),
                preparationSteps = listOf("Marinate chicken in lemon juice, garlic, oregano, block-salt, pan grill for 12m.", "Serve on bed of hot rice and steam vegetables.", "Drizzle extra virgin olive oil before consuming.")
            ),
            dinner = Meal(
                name = "Elite Pan-Seared Atlantic Salmon & Asparagus",
                calories = mealCal - 50,
                protein = (p * 0.25f),
                carbs = if (dietTypeName.lowercase().contains("keto")) 5f else (c * 0.20f),
                fats = (f * 0.30f),
                ingredients = listOf("Fresh Salmon Fillet (180g)", "Baby Asparagus spears (100g)", "Sweet Potato baked (100g)", "Fresh lemon & garlic squeeze"),
                preparationSteps = listOf("Preheat air-fryer, season salmon with salt, black pepper, and lemon slice.", "Cook for 11 minutes at 190 degrees until golden outer crust.", "Pan-sear asparagus spears with direct garlic spray and plate.")
            ),
            snack = Meal(
                name = "Post-Workout Muscle Recovery Shake",
                calories = mealCal - 100,
                protein = (p * 0.15f),
                carbs = if (dietTypeName.lowercase().contains("keto")) 3f else (c * 0.15f),
                fats = 6f,
                ingredients = listOf("1 Scoop Iso-Whey Protein", "Organic Almond Butter (1 tbsp)", "Semi-skimmed milk or Almond juice", "Cinnamon powder"),
                preparationSteps = listOf("Combine whey protein, milk, and almond butter in shaker cup.", "Add 100ml cold water, ice cubes, and cinnamon powder.", "Shake firmly for 20 seconds and consume within 45m of training.")
            )
        )
    }
}
