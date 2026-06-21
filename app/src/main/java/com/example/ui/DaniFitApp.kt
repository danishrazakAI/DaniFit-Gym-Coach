package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.VoltPrimary
import com.example.ui.theme.VoltSecondary
import com.example.ui.theme.VoltSurface
import com.example.ui.theme.VoltTertiary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaniFitApp(viewModel: DaniFitViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (userProfile == null || !userProfile!!.onboarded) {
            OnboardingWizardScreen(viewModel = viewModel)
        } else {
            MainAppLayout(profile = userProfile!!, viewModel = viewModel)
        }
    }
}

// ========================
// ONBOARDING WIZARD SCREEN
// ========================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingWizardScreen(viewModel: DaniFitViewModel) {
    var step by remember { mutableStateOf(1) }
    
    // Step 1: Bio
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("26") }
    var gender by remember { mutableStateOf("Male") }
    
    // Step 2: Body Metrics
    var height by remember { mutableStateOf("175") }
    var weight by remember { mutableStateOf("72") }
    var bodyFat by remember { mutableStateOf("18") }
    
    // Step 3: Fitness Goals & Level
    var fitnessLevel by remember { mutableStateOf("Intermediate") }
    val levels = listOf("Beginner", "Intermediate", "Advanced")
    var fitnessGoal by remember { mutableStateOf("Fat Loss") }
    val goals = listOf("Weight Loss", "Fat Loss", "Muscle Gain", "Body Recomposition", "Strength Building", "Endurance", "General Fitness")
    
    // Step 4: Medical / Lifestyle
    var dailyActivity by remember { mutableStateOf("Moderately Active") }
    val activities = listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active")
    var gymAccess by remember { mutableStateOf("Gym & Home") }
    var injuries by remember { mutableStateOf("None") }
    var availableEquipment by remember { mutableStateOf("Dumbbells, Pull-up Bar, Bench") }
    var daysPerWeek by remember { mutableStateOf("4") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "DaniFit Coach",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 24.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step Progress Indicators
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (i in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i <= step) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                // Subtitle
                Text(
                    text = when(step) {
                        1 -> "Who are you training today?"
                        2 -> "Current Body Composition"
                        3 -> "Your Fitness Aspirations"
                        else -> "Lifestyle & Safety Habits"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Input Layouts per step
                when (step) {
                    1 -> {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("input_onboarding_name"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )

                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("Age (Years)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                        )

                        Text("Select Gender", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Male", "Female", "Non-binary").forEach { g ->
                                val selected = gender == g
                                Button(
                                    onClick = { gender = g },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(g)
                                }
                            }
                        }
                    }

                    2 -> {
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it },
                            label = { Text("Height (cm)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.Height, contentDescription = null) }
                        )

                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Weight (kg)") },
                            placeholder = { Text("e.g. 72") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) }
                        )

                        OutlinedTextField(
                            value = bodyFat,
                            onValueChange = { bodyFat = it },
                            label = { Text("Estimated Body Fat % (Optional)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null) }
                        )
                    }

                    3 -> {
                        Text("Fitness Level", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                        levels.forEach { level ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (fitnessLevel == level) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                    .clickable { fitnessLevel = level }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (fitnessLevel == level), onClick = { fitnessLevel = level })
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(level, style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Select Core Goal", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                        goals.forEach { goalOption ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (fitnessGoal == goalOption) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                    .clickable { fitnessGoal = goalOption }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (fitnessGoal == goalOption), onClick = { fitnessGoal = goalOption })
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(goalOption, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    4 -> {
                        Text("Activity Level multiplier", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                        activities.forEach { act ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (dailyActivity == act) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                    .clickable { dailyActivity = act }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (dailyActivity == act), onClick = { dailyActivity = act })
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(act, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = injuries,
                            onValueChange = { injuries = it },
                            label = { Text("Medical conditions / Allergies / Injuries") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            placeholder = { Text("e.g. knee injury, nut allergy, none") }
                        )

                        Text("Gym Access", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Gym & Home", "Home Workout Only").forEach { access ->
                                val selected = gymAccess == access
                                Button(
                                    onClick = { gymAccess = access },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(access)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = availableEquipment,
                            onValueChange = { availableEquipment = it },
                            label = { Text("Available Equipment (separated by commas)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = daysPerWeek,
                            onValueChange = { daysPerWeek = it },
                            label = { Text("Workout Days per Week") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text("Back")
                    }
                }

                Button(
                    onClick = {
                        if (step < 4) {
                            step++
                        } else {
                            // Validate and Submit
                            if (name.trim().isNotEmpty()) {
                                viewModel.completeOnboarding(
                                    name = name,
                                    age = age.toIntOrNull() ?: 26,
                                    gender = gender,
                                    height = height.toFloatOrNull() ?: 175f,
                                    weight = weight.toFloatOrNull() ?: 72f,
                                    bodyFat = bodyFat.toFloatOrNull(),
                                    fitnessLevel = fitnessLevel,
                                    fitnessGoal = fitnessGoal,
                                    medicalConditions = injuries,
                                    lifestyleActivity = dailyActivity,
                                    gymAccess = gymAccess,
                                    availableEquipment = availableEquipment,
                                    workoutDaysPerWeek = daysPerWeek.toIntOrNull() ?: 4
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(start = 8.dp).testTag("button_onboarding_next"),
                    enabled = (step != 1 || name.trim().isNotEmpty())
                ) {
                    Text(if (step == 4) "Calculate Assessment" else "Continue")
                }
            }
        }
    }
}

// ========================
// MAIN NAVIGATION LAYOUT
// ========================
@Composable
fun MainAppLayout(profile: UserProfile, viewModel: DaniFitViewModel) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(if (selectedTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Insights") },
                    modifier = Modifier.testTag("tab_insights")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(if (selectedTab == 1) Icons.Filled.Assignment else Icons.Outlined.Assignment, contentDescription = "Plans") },
                    label = { Text("Plans") },
                    modifier = Modifier.testTag("tab_plans")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(if (selectedTab == 2) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat, contentDescription = "Coach Chat") },
                    label = { Text("Coach Chat") },
                    modifier = Modifier.testTag("tab_coach")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(if (selectedTab == 3) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents, contentDescription = "Gamification") },
                    label = { Text("Arena") },
                    modifier = Modifier.testTag("tab_arena")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardTab(profile = profile, viewModel = viewModel)
                1 -> PlannerTab(profile = profile, viewModel = viewModel)
                2 -> ChatbotTab(profile = profile, viewModel = viewModel)
                3 -> GamificationTab(profile = profile, viewModel = viewModel)
            }
        }
    }
}

// ========================
// INSIGHTS / DASHBOARD TAB
// ========================
@Composable
fun DashboardTab(profile: UserProfile, viewModel: DaniFitViewModel) {
    val stepLog by viewModel.todayStepLog.collectAsStateWithLifecycle()
    val totalWorkouts by viewModel.totalWorkoutsCompleted.collectAsStateWithLifecycle()
    val totalCaloriesSpent by viewModel.totalCaloriesBurned.collectAsStateWithLifecycle()
    val stepLogsList by viewModel.stepLogs.collectAsStateWithLifecycle()

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "GOOD MORNING"
            in 12..16 -> "GOOD AFTERNOON"
            else -> "GOOD EVENING"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Bento Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF414941),
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "DaniFit Coach",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF191C19),
                    letterSpacing = (-0.5).sp
                )
            }
            var showIntegrationsDialog by remember { mutableStateOf(false) }
            val integrationsList by viewModel.integrations.collectAsStateWithLifecycle()
            val anyConnected = integrationsList.any { it.isConnected }
            val context = LocalContext.current

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cloud Sync Integrations Button (Tactile bento accent)
                IconButton(
                    onClick = { showIntegrationsDialog = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (anyConnected) Color(0xFF386B3F) else Color(0xFFE2F0D8))
                        .testTag("button_integrations_main")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Fit Integrations Dashboard",
                        tint = if (anyConnected) Color.White else Color(0xFF386B3F),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Existing User Avatar Circle matching JD
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD8E7D4))
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = profile.name.split(" ")
                        .mapNotNull { it.firstOrNull() }
                        .joinToString("")
                        .take(2)
                        .uppercase()
                    Text(
                        text = initials.ifEmpty { "JD" },
                        color = Color(0xFF386B3F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            if (showIntegrationsDialog) {
                AlertDialog(
                    onDismissRequest = { showIntegrationsDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color(0xFF386B3F),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Sync Center & Integrations",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191C19)
                            )
                        }
                    },
                    text = {
                        var showAppleTokenDialog by remember { mutableStateOf(false) }
                        var appleTokenInput by remember { mutableStateOf("apple_healthkit_live_token") }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Federate and sync your daily fitness activity, workouts, and local pedometer step data natively on your Android device.",
                                fontSize = 12.sp,
                                color = Color(0xFF414941).copy(alpha = 0.8f)
                            )
                            
                            integrationsList.forEach { item ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE0E4DB)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val (serviceIcon, serviceColor) = when (item.serviceId) {
                                                    "youtube" -> Pair(Icons.Default.PlayCircle, Color(0xFFFF0000))
                                                    "google_fit" -> Pair(Icons.Default.DirectionsWalk, Color(0xFF4285F4))
                                                    "android_sensor" -> Pair(Icons.Default.FitnessCenter, Color(0xFF386B3F))
                                                    "apple_health" -> Pair(Icons.Default.Favorite, Color(0xFFE91E63))
                                                    else -> Pair(Icons.Default.Settings, Color(0xFF386B3F))
                                                }
                                                Icon(
                                                    imageVector = serviceIcon,
                                                    contentDescription = null,
                                                    tint = serviceColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = item.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF191C19)
                                                    )
                                                    Text(
                                                        text = if (item.isConnected) "Active" else "Inactive",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (item.isConnected) Color(0xFF386B3F) else Color(0xFFBA1A1A)
                                                    )
                                                }
                                            }
                                            
                                            // Action Button
                                            if (item.isConnected) {
                                                TextButton(
                                                    onClick = { viewModel.disconnectIntegration(item.serviceId) },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFBA1A1A)),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        when (item.serviceId) {
                                                            "youtube" -> {
                                                                viewModel.connectYouTube()
                                                            }
                                                            "google_fit" -> {
                                                                viewModel.syncGoogleFit(context)
                                                            }
                                                            "android_sensor" -> {
                                                                viewModel.connectLocalStepSensor()
                                                            }
                                                            "apple_health" -> {
                                                                showAppleTokenDialog = true
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF386B3F),
                                                        contentColor = Color.White
                                                    ),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(if (item.serviceId == "google_fit") "Sync" else if (item.serviceId == "apple_health") "Connect" else "Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        
                                        // Detail description & sync log
                                        Text(
                                            text = item.syncMessage,
                                            fontSize = 11.sp,
                                            color = Color(0xFF414941).copy(alpha = 0.8f)
                                        )
                                        
                                        if (item.lastSyncTimestamp > 0) {
                                            val lastSyncDate = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.lastSyncTimestamp))
                                            Text(
                                                text = "Last Synced: $lastSyncDate",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF414941).copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Apple HealthKit Secure Token Overlay
                        if (showAppleTokenDialog) {
                            AlertDialog(
                                onDismissRequest = { showAppleTokenDialog = false },
                                title = { Text("Apple HealthKit Bridge", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            "Secure federation uses personal iCloud Health containers to link Apple Watch / HealthKit steps securely on Android.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF414941)
                                        )
                                        OutlinedTextField(
                                            value = appleTokenInput,
                                            onValueChange = { appleTokenInput = it },
                                            label = { Text("iCloud Health Auth Token") },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            viewModel.syncAppleHealth(appleTokenInput)
                                            showAppleTokenDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B3F))
                                    ) {
                                        Text("Authenticate & Sync", fontSize = 11.sp)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAppleTokenDialog = false }) {
                                        Text("Cancel", fontSize = 11.sp)
                                    }
                                }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showIntegrationsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B3F))
                        ) {
                            Text("Done", fontSize = 12.sp)
                        }
                    }
                )
            }
        }

        // Row 1: Steps & Secondary Stats (Grid side-by-side style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Steps Bento Card (Left, takes more weight)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F0D8)),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .weight(1.35f)
                    .height(205.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val liveLog = stepLog ?: StepLog("", 0, 10000)
                    val percentage = if (liveLog.targetSteps > 0) (liveLog.stepsCount.toFloat() / liveLog.targetSteps.toFloat()).coerceIn(0f, 1f) else 0f
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.5f))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = "Steps tracker icon",
                                tint = Color(0xFF386B3F),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF386B3F))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "DAILY GOAL",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Column {
                        val formattedSteps = String.format(Locale.getDefault(), "%,d", liveLog.stepsCount)
                        Text(
                            text = formattedSteps,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF191C19),
                            letterSpacing = (-1.2).sp
                        )
                        Text(
                            text = "Steps Taken Today",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF414941).copy(alpha = 0.7f)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.4f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(percentage)
                                    .clip(CircleShape)
                                    .background(Color(0xFF386B3F))
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format("%.2f km", liveLog.stepsCount * 0.00075f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF414941)
                            )
                            Text(
                                text = "${liveLog.stepsCount / 100} mins active",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF414941)
                            )
                        }
                    }
                }
            }

            // Calories & Workout counter (Right column stack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(205.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Calories Burnt Card (White with delicate border)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.2.dp, Color(0xFFE0E4DB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Calories burnt icon",
                            tint = Color(0xFF386B3F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$totalCaloriesSpent",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C19)
                        )
                        Text(
                            text = "KCAL BURNT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF414941).copy(alpha = 0.8f),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Workouts Card (Hydration Sky Blue bento cell matching HTML)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD3E4FF)),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = "Workouts sessions icon",
                            tint = Color(0xFF001D36),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$totalWorkouts Done",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001D36)
                        )
                        Text(
                            text = "WORKOUT STREAK",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001D36).copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Row 2: AI Coach Suggester & Assessments (Charcoal Card row)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191C19)),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header of coaching details
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF386B3F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI Assistant icon",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "AI COACH SUGGESTIONS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA9E9A5),
                            letterSpacing = 0.8.sp
                        )
                        val adviceText = when {
                            profile.fitnessGoal.contains("Gain", ignoreCase = true) -> 
                                "Focus: Muscle recovery needs high protein. Optimize clean calorie surpluses today."
                            profile.fitnessGoal.contains("Loss", ignoreCase = true) || profile.fitnessGoal.contains("Fat", ignoreCase = true) -> 
                                "Tip: Hydrate before training. Your targets represent a clean daily calorie deficit."
                            else -> 
                                "Advice: Fuel clean energy via structured fats and protein goals."
                        }
                        Text(
                            text = adviceText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 10.dp))

                // BMI statistics
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Body Mass Index (BMI)",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format("%.1f", profile.bmi),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                            Text(
                                text = " kg/m²",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("IDEAL RANGE", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA9E9A5), fontWeight = FontWeight.Bold)
                            Text("18.5 - 24.9", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Smooth mini slider progress representation of BMI
                val bmiPosition = (profile.bmi / 40f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF29B6F6), Color(0xFF66BB6A), Color(0xFFFFEE58), Color(0xFFEF5350))
                            )
                        )
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 14.dp)
                        .height(8.dp)
                ) {
                    val x = size.width * bmiPosition
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, 4.dp.toPx()))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("BMR Requirements", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        Text("${String.format("%.0f", profile.bmr)} kcal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Recommended Calorie Intake", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        Text("${String.format("%.0f", profile.dailyCalorieNeeds)} kcal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA9E9A5))
                    }
                }

                // Bento-style macro nutrition pill tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MacroBentoBadge(label = "PROTEIN", value = "${profile.macroProtein.toInt()}g", color = Color(0xFFE2F0D8))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MacroBentoBadge(label = "CARBS", value = "${profile.macroCarbs.toInt()}g", color = Color(0xFFD3E4FF))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MacroBentoBadge(label = "FATS", value = "${profile.macroFats.toInt()}g", color = Color(0xFFF8BBD0))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MacroBentoBadge(label = "WATER", value = "${String.format("%.1f", profile.waterIntakeRequirement)}L", color = Color.White)
                    }
                }
            }
        }

        // Row 3: Historical Trend + Simulators (White card outline style matching Bento HTML)
        if (stepLogsList.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.2.dp, Color(0xFFE0E4DB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Historical Step Trend",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF191C19)
                    )
                    Text(
                        text = "Weekly Activity Logs",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF414941).copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    val maxSteps = stepLogsList.maxOfOrNull { it.stepsCount } ?: 10000
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(95.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        stepLogsList.take(6).reversed().forEach { log ->
                            val heightRatio = if (maxSteps > 0) log.stepsCount.toFloat() / maxSteps.toFloat() else 0f
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .fillMaxHeight(fraction = heightRatio.coerceIn(0.08f, 1.0f))
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                                        .background(
                                            if (log.stepsCount >= log.targetSteps) Color(0xFF386B3F)
                                            else Color(0xFFD3E4FF)
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Formats Date
                                val formattedDate = try {
                                    val originalFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val date = originalFormat.parse(log.date)
                                    SimpleDateFormat("dd/MM", Locale.getDefault()).format(date!!)
                                } catch (e: Exception) {
                                    log.date
                                }
                                Text(
                                    text = formattedDate,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF414941)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFFE0E4DB), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Step Tracking Simulators",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C19)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateSteps(500) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE2F0D8),
                                contentColor = Color(0xFF191C19)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).testTag("button_simulate_steps_500")
                        ) {
                            Text("+500 Steps", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { viewModel.simulateSteps(2000) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE2F0D8),
                                contentColor = Color(0xFF191C19)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).testTag("button_simulate_steps_2000")
                        ) {
                            Text("+2,000 Steps", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MacroBentoBadge(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StepValueItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AssessmentField(label: String, value: String, sub: String) {
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(sub, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MacroCircle(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(1.5.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ========================
// PLANNING TAB (WORKOUT & DIET)
// ========================
@Composable
fun ActiveExerciseSessionCard(
    item: ExerciseSessionState,
    viewModel: DaniFitViewModel
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.playExerciseVideo(context, item.exerciseName) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, if (item.isCompleted) VoltPrimary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isCompleted) VoltPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        if (item.isCompleted) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = VoltPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "Target Muscle: ${item.targetMuscle}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = { viewModel.playExerciseVideo(context, item.exerciseName) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFF0000).copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Watch Guide",
                        tint = Color(0xFFFF0000),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = "Target sets: 3 | Standard guide: ${item.defaultReps} reps",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            if (item.isTimerRunning && item.restSecondsRemaining > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(VoltPrimary.copy(alpha = 0.15f))
                        .border(1.dp, VoltPrimary, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { item.restSecondsRemaining.toFloat() / item.restTimeSeconds.toFloat() },
                            color = VoltPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "⌛ Rest Countdown: ${item.restSecondsRemaining}s remain...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoltPrimary
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Rest",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Interval Rest window: ${item.restTimeSeconds}s between sets",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = item.repsLogged,
                    onValueChange = { viewModel.updateSessionExerciseLogged(item.exerciseName, it, item.weightLoggedKg) },
                    label = { Text("Reps", fontSize = 9.sp) },
                    placeholder = { Text("12") },
                    modifier = Modifier
                        .width(90.dp)
                        .testTag("input_reps_${item.exerciseName.replace(" ", "_")}"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = item.weightLoggedKg,
                    onValueChange = { viewModel.updateSessionExerciseLogged(item.exerciseName, item.repsLogged, it) },
                    label = { Text("Weight (Kg)", fontSize = 9.sp) },
                    placeholder = { Text("20") },
                    modifier = Modifier
                        .width(110.dp)
                        .testTag("input_weight_${item.exerciseName.replace(" ", "_")}"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = { viewModel.toggleExerciseCompleted(item.exerciseName) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.isCompleted) Color(0xFF386B3F) else VoltPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("button_complete_${item.exerciseName.replace(" ", "_")}")
                ) {
                    Text(
                        text = if (item.isCompleted) "Done ✓" else "Log Set",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PlannerTab(profile: UserProfile, viewModel: DaniFitViewModel) {
    var plannerSubTab by remember { mutableStateOf(0) } // 0: Workouts, 1: Diet Nutrition
    val currentWorkoutPlan by viewModel.currentWorkoutPlan.collectAsStateWithLifecycle()
    val currentDietPlan by viewModel.currentDietPlan.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingPlan.collectAsStateWithLifecycle()

    val selectedSplit by viewModel.selectedWorkoutSplit.collectAsStateWithLifecycle()
    val levelOverride by viewModel.selectedFitnessLevelOverride.collectAsStateWithLifecycle()
    val isSessionActive by viewModel.isWorkoutSessionActive.collectAsStateWithLifecycle()
    val sessionElapsed by viewModel.workoutSessionElapsedSeconds.collectAsStateWithLifecycle()
    val sessionExercises by viewModel.workoutSessionExercises.collectAsStateWithLifecycle()
    val sessionSummary by viewModel.activeSessionSummary.collectAsStateWithLifecycle()
    var showFeelingDialog by remember { mutableStateOf(false) }

    var activePantryIngredients by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (currentWorkoutPlan == null) {
            viewModel.generatePlans()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toggle Buttons
        TabRow(selectedTabIndex = plannerSubTab) {
            Tab(
                selected = plannerSubTab == 0,
                onClick = { plannerSubTab = 0 },
                text = { Text("🏋️ Workout Generator") }
            )
            Tab(
                selected = plannerSubTab == 1,
                onClick = { plannerSubTab = 1 },
                text = { Text("🥗 AI Diet Planner") }
            )
        }

        if (isGenerating) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (plannerSubTab == 0) {
                    // WORKOUT RETRIEVAL & CATEGORY SELECTION
                    if (!isSessionActive) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("🎯 Routine Calibration Splits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Select your primary compound target split. Movements auto-calibrate to your chosen fitness capacity level.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    
                                    // Row split selectors
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("Push", "Pull").forEach { split ->
                                            Button(
                                                onClick = { viewModel.setWorkoutSplit(split) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (selectedSplit == split) VoltPrimary else MaterialTheme.colorScheme.surface,
                                                    contentColor = if (selectedSplit == split) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                border = BorderStroke(1.dp, if (selectedSplit == split) VoltPrimary else MaterialTheme.colorScheme.outlineVariant),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(44.dp).testTag("split_${split.lowercase()}")
                                            ) {
                                                Text(if (split == "Push") "🏋️ Push Day" else "💪 Pull Day", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("💪 Fitness Level suggestions filter:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    
                                    // Row level overrides
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val activeLevel = levelOverride ?: profile.fitnessLevel
                                        listOf("Beginner", "Intermediate", "Advanced").forEach { lvl ->
                                            Button(
                                                onClick = { viewModel.setFitnessLevelOverride(lvl) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (activeLevel == lvl) VoltSecondary else MaterialTheme.colorScheme.surface,
                                                    contentColor = if (activeLevel == lvl) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                border = BorderStroke(1.dp, if (activeLevel == lvl) VoltSecondary else MaterialTheme.colorScheme.outlineVariant),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(36.dp).testTag("level_override_${lvl.lowercase()}"),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(lvl, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Split explanation notes
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = if (selectedSplit == "Push") {
                                                "🔥 Push targets: Pectorals (Chest), Anterior Deltoids (Shoulders), and Triceps. Recommended for structural chest density and overhead vertical power."
                                            } else {
                                                "🧬 Pull targets: Latissimus (Back), Rhomboids/Traps, Rear Deltoids, and Biceps. Essential for spinal posture health, grip force, and arm flexion curls."
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Toggle button to Start Workout
                        item {
                            Button(
                                onClick = { viewModel.startWorkoutSession() },
                                colors = ButtonDefaults.buttonColors(containerColor = VoltPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("button_start_workout_session")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                    Text("Start $selectedSplit Workout Session 🏋️", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }

                        val activeLevel = levelOverride ?: profile.fitnessLevel
                        val suggestedExercisesList = viewModel.getPushPullExercises(selectedSplit, activeLevel)

                        item {
                            Text(
                                text = "Suggested Movements & Interactive Videos (${suggestedExercisesList.size} Exercises)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(suggestedExercisesList) { exe ->
                            ExerciseItemCard(exe = exe, viewModel = viewModel)
                        }
                    } else {
                        // ACTIVE WORKOUT SESSION STARTED
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = VoltPrimary.copy(alpha = 0.15f)),
                                border = BorderStroke(1.5.dp, VoltPrimary)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("⏱️ Active Workout Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VoltPrimary)
                                        Text("Current: $selectedSplit Split Program (${levelOverride ?: profile.fitnessLevel})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    
                                    val minutes = sessionElapsed / 60
                                    val seconds = sessionElapsed % 60
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(VoltPrimary)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = String.format("%02d:%02d", minutes, seconds),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Movements Logging & Verification",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(sessionExercises) { exe ->
                            ActiveExerciseSessionCard(item = exe, viewModel = viewModel)
                        }

                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showFeelingDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B3F)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(54.dp).testTag("button_finish_workout_session")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Text("Save, Record & Finish Workout 🏆", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                } else {
                    // DIET NUTRITION
                    val diet = currentDietPlan
                    if (diet == null) {
                        item {
                            EmptyStateCard(
                                message = "No dietary roadmap created yet. Let's design your macro targets!",
                                onAction = { viewModel.generatePlans() }
                            )
                        }
                    } else {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Macro Meal Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("Diet Program: ${diet.planName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.generatePlans() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                                }
                            }
                        }

                        // Breakfast, Lunch, Dinner, Snack Cards
                        item { MealCategoryCard(title = "🍳 Breakfast", meal = diet.breakfast) }
                        item { MealCategoryCard(title = "🍗 Lunch Routine", meal = diet.lunch) }
                        item { MealCategoryCard(title = "🐟 Balanced Dinner", meal = diet.dinner) }
                        item { MealCategoryCard(title = "🥛 Healthy Snack / Shakes", meal = diet.snack) }

                        // PANTRY INGREDIENT SUGGESTION SECION
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Kitchen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Pantry Chef Meal Suggester", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Enter your leftovers or available fridge ingredients, and DaniFit Coach will formulate a customized macro recipe recipe for you instantly! Answers are posted directly in Coach Chat tab.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = activePantryIngredients,
                                        onValueChange = { activePantryIngredients = it },
                                        placeholder = { Text("e.g. eggs, spinach, brown rice, turkey slice") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            if (activePantryIngredients.trim().isNotEmpty()) {
                                                viewModel.directSuggestRecipe(activePantryIngredients)
                                                activePantryIngredients = ""
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.End).testTag("button_get_pantry_recipe")
                                    ) {
                                        Text("🍳 Get AI Recipes")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // OVERLAYS & PROGRESS DIALOGS
    if (showFeelingDialog) {
        AlertDialog(
            onDismissRequest = { showFeelingDialog = false },
            title = { Text("Rate Today's Effort", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Select how you physically and mentally felt during this physical performance session:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    val feelings = listOf(
                        Pair("🥵 Exhausted", "Exhausted"),
                        Pair("💪 Strong", "Strong"),
                        Pair("⚡ Energetic", "Energetic"),
                        Pair("😴 Sluggish", "Sluggish"),
                        Pair("😊 Balanced", "Good & Balanced")
                    )
                    
                    feelings.forEach { (label, value) ->
                        Button(
                            onClick = {
                                viewModel.finishWorkoutSession(value)
                                showFeelingDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("feeling_${value.lowercase()}"),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFeelingDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    if (sessionSummary != null) {
        val summary = sessionSummary!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissSessionSummary() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🏆 Session Achievements!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = VoltPrimary)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Congratulations! Your session statistics have been added and synced to persistent database history.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Program Split:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${summary.splitType} Split", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Time Consumed:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${summary.timeMinutes} min ${summary.timeSeconds} sec", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Calories Burned:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${summary.caloriesBurned} kcal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Exercises Done:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${summary.exercisesCompleted} movements", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Rate Feeling:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(summary.feelingEmojiSelected, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("🏅 Personal Records Evaluation", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    
                    if (summary.prsEarned.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            summary.prsEarned.forEach { pr ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF386B3F).copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, Color(0xFF386B3F)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("🎉 NEW BEST UNLOCKED!", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF386B3F))
                                        Text(pr.exerciseName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("Max load: ${pr.newRecordKg} Kg (Previous: ${pr.previousRecordKg} Kg)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Text(
                                "No new weight load records today, but incredible volume logged! Consistent load overload breeds long-term muscular hypertrophy. 🚀",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissSessionSummary() },
                    colors = ButtonDefaults.buttonColors(containerColor = VoltPrimary),
                    modifier = Modifier.testTag("button_dismiss_summary")
                ) {
                    Text("Double down & proceed", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ExerciseItemCard(exe: Exercise, viewModel: DaniFitViewModel) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Exercise Title and stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(exe.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Target Muscle: ${exe.targetMuscle}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Text("${exe.caloriesBurned} kcal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ripped stats line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sets: ${exe.sets}", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DoubleArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reps: ${exe.reps}", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rest: ${exe.restTimeSeconds}s", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Demonstration Video Recommendations
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.playExerciseVideo(context, exe.name) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Video Thumbnail Mock
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Tutorial video",
                            tint = VoltPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Demonstration Tutorial Video", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Duration: ${exe.videoDuration} | Equip: ${exe.equipmentRequired}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(exe.formInstructions, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("▶ Tap to play video", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = VoltPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Alternative Exercise swaps & alterations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (exe.altExercise != null) {
                    OutlinedButton(
                        onClick = { viewModel.loadAlternativeForExercise(exe.name) },
                        modifier = Modifier.weight(1.2f).height(38.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SwapCalls, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Swap Alternate", fontSize = 10.sp)
                        }
                    }
                }
                if (exe.beginnerVariation != null) {
                    OutlinedButton(
                        onClick = { viewModel.setBeginnerVariation(exe.name) },
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Beginner Tab", fontSize = 10.sp)
                    }
                }
                if (exe.advancedVariation != null) {
                    Button(
                        onClick = { viewModel.setAdvancedVariation(exe.name) },
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color.Black)
                    ) {
                        Text("Go Advanced", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MealCategoryCard(title: String, meal: Meal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Text("${meal.calories} kcal", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            Text(meal.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 4.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NutritionTag(label = "Protein", value = "${meal.protein.toInt()}g", color = VoltPrimary)
                NutritionTag(label = "Carbs", value = "${meal.carbs.toInt()}g", color = VoltSecondary)
                NutritionTag(label = "Fats", value = "${meal.fats.toInt()}g", color = VoltTertiary)
            }

            // Ingredients
            Text("Ingredients", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(meal.ingredients.joinToString(", "), fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))

            // Prep
            Text("Preparation Steps", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            meal.preparationSteps.forEachIndexed { index, step ->
                Text("${index + 1}. $step", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
fun NutritionTag(label: String, value: String, color: Color) {
    Row {
        Box(
            modifier = Modifier
                .size(8.dp)
                .align(Alignment.CenterVertically)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("$label: $value", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyStateCard(message: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.DynamicFeed, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text("Generate Plan Now")
            }
        }
    }
}

// ========================
// AI TRAINER CHATBOT TAB
// ========================
@Composable
fun ChatbotTab(profile: UserProfile, viewModel: DaniFitViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    val chatInput by viewModel.chatInput.collectAsStateWithLifecycle()
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Quick trigger questions
    val quickTriggers = listOf(
        "Suggest a quick glute workout",
        "How much protein should I consume?",
        "I have minor knee issues—modify exercises",
        "Suggest a 1800 Cal high-protein plan"
    )

    Column(modifier = Modifier.fillMaxSize().imePadding().padding(16.dp)) {
        // Chat Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("DaniFit AI Coach", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Text("Active Nutritionist & Personal Trainer", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = { viewModel.clearChatHistory() }) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Session")
            }
        }

        // Quick triggers Row
        Text("Quick Training Prompts", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            items(quickTriggers) { trig ->
                SuggestionChip(
                    onClick = {
                        viewModel.updateChatInput(trig)
                        viewModel.sendChatMessage()
                    },
                    label = { Text(trig, fontSize = 11.sp) }
                )
            }
        }

        // Horizontal line separator
        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

        // Chats Scrollable area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                messages.forEach { msg ->
                    ChatBubble(msg = msg)
                }
                if (isChatLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
            
            // Auto scroll to bottom
            LaunchedEffect(messages.size, isChatLoading) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }

        // Search Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { viewModel.updateChatInput(it) },
                placeholder = { Text("Ask Coach DaniFit anything...") },
                modifier = Modifier.weight(1f).testTag("chat_input_field"),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = { viewModel.sendChatMessage() },
                modifier = Modifier.size(52.dp).testTag("chat_send_button"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send Message", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.sender == "user"
    val isSystem = msg.sender == "system"

    if (isSystem) {
        // Action Congrats message
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(msg.message, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(VoltPrimary).align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Sports, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        msg.message,
                        fontSize = 13.sp,
                        color = if (isUser) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ========================
// ARENA / GAMIFICATION TAB
// ========================
@Composable
fun GamificationTab(profile: UserProfile, viewModel: DaniFitViewModel) {
    val stepsCalculated by viewModel.todayStepLog.collectAsStateWithLifecycle()
    val currentSteps = stepsCalculated?.stepsCount ?: 0
    val totalWorkouts by viewModel.totalWorkoutsCompleted.collectAsStateWithLifecycle()

    var showRemindersState by remember { mutableStateOf(false) }

    // Dummy remind variables
    var remindWorkout by remember { mutableStateOf(true) }
    var remindWater by remember { mutableStateOf(true) }
    var remindMeal by remember { mutableStateOf(false) }
    var remindSleep by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headline
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Coach Arena", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Level up your lifestyle!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = { showRemindersState = !showRemindersState },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reminders")
                }
            }
        }

        // Reminders expandable dropdown
        AnimatedVisibility(visible = showRemindersState) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daily Routine Reminders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    RemindToggle(label = "Workout Time Reminder", checked = remindWorkout, onCheckedChange = { remindWorkout = it })
                    RemindToggle(label = "Hydration / Water Intake Reminder", checked = remindWater, onCheckedChange = { remindWater = it })
                    RemindToggle(label = "Healthy Meal Timing Alert", checked = remindMeal, onCheckedChange = { remindMeal = it })
                    RemindToggle(label = "Sleep and Recovery Alert", checked = remindSleep, onCheckedChange = { remindSleep = it })
                }
            }
        }

        // 1. Leaderboard Ranking
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Global Challenge Leaderboard", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = VoltPrimary)
                }
                Text("Steps and Activity leaderboard with real athletes.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(modifier = Modifier.height(16.dp))

                // Danish rating dynamically adjusted
                val ourStepsPercent = currentSteps.coerceAtLeast(0)
                val leaderboardList = listOf(
                    LeaderboardUser(rank = 1, name = "Alexander K.", steps = 12400, highlight = false),
                    LeaderboardUser(rank = 2, name = "${profile.name} (You)", steps = ourStepsPercent, highlight = true),
                    LeaderboardUser(rank = 3, name = "Tiffany Rose", steps = 8900, highlight = false),
                    LeaderboardUser(rank = 4, name = "Brandon S.", steps = 7200, highlight = false),
                    LeaderboardUser(rank = 5, name = "Chloe G.", steps = 4800, highlight = false)
                ).sortedByDescending { it.steps }

                leaderboardList.forEachIndexed { idx, user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (user.highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                                else Color.Transparent
                            )
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("#${idx + 1}", fontWeight = FontWeight.Bold, color = if (user.highlight) VoltPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(user.name, fontWeight = if (user.highlight) FontWeight.Bold else FontWeight.Normal)
                        }
                        Text("${user.steps} steps", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Achievements & Badges Center
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Achievements & Unlockable Badges", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Gamification points collected in your training cycles", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(modifier = Modifier.height(16.dp))

                val is10kUnlocked = currentSteps >= 10000
                val isStreakUnlocked = totalWorkouts >= 1
                val isCenturyUnlocked = totalWorkouts >= 5

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BadgeItem(
                        label = "10k Step Cup",
                        subtitle = "Steps goal met",
                        unlocked = is10kUnlocked,
                        icon = Icons.Default.EmojiEvents,
                        activeColor = VoltPrimary
                    )
                    BadgeItem(
                        label = "Day 1 Streak",
                        subtitle = "Completed routine",
                        unlocked = isStreakUnlocked,
                        icon = Icons.Default.LocalFireDepartment,
                        activeColor = VoltTertiary
                    )
                    BadgeItem(
                        label = "Century Peak",
                        subtitle = "5+ workouts done",
                        unlocked = isCenturyUnlocked,
                        icon = Icons.Default.FlashOn,
                        activeColor = VoltSecondary
                    )
                }
            }
        }

        // 3. Current active social group challenges
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Joinable Active Arena Challenges", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Compete with others globally to hit targets", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(12.dp))

                ActiveChallengeItem(title = "🌞 Summer Calorie Burner", target = "12,000 kcal total", members = "242 Active", progress = 0.4f)
                ActiveChallengeItem(title = "💧 3L Hydration League", target = "Hit 3L daily for 7 days", members = "98 Active", progress = 0.8f)
                ActiveChallengeItem(title = "⛰️ Steps Marathon", target = "Daily 10k runners", members = "1.2k Active", progress = (currentSteps.toFloat()/10000f).coerceIn(0f, 1f))
            }
        }
    }
}

data class LeaderboardUser(val rank: Int, val name: String, val steps: Int, val highlight: Boolean)

@Composable
fun BadgeItem(label: String, subtitle: String, unlocked: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, activeColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (unlocked) activeColor.copy(alpha = 0.2f) 
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (unlocked) activeColor else Color.Gray
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun ActiveChallengeItem(title: String, target: String, members: String, progress: Float) {
    var joined by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Goal: $target | $members", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(
                onClick = { joined = !joined },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (joined) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (joined) "Leave" else "Join Cup")
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun RemindToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
