package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.api.GeminiApi
import com.example.ui.components.BodyCanvas
import com.example.ui.models.BodyPart
import com.example.ui.models.BodySystem
import com.example.ui.models.BodyView
import com.example.ui.models.PredefinedData
import com.example.ui.models.StudyProgress
import kotlinx.coroutines.launch

enum class AppMode {
    STUDY, QUIZ, CUSTOM_EDITOR, STATS
}

enum class QuizType {
    TAP_TARGET, // App gives a name, user clicks correct pinpoint
    IDENTIFY_PIN // App highlights a pinpoint, user guesses the name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnatomyApp() {
    val coroutineScope = rememberCoroutineScope()
    
    // Core Navigation & Selection States
    var selectedMode by rememberSaveable { mutableStateOf(AppMode.STUDY) }
    var selectedSystem by rememberSaveable { mutableStateOf(BodySystem.EXTERNAL) }
    var selectedView by rememberSaveable { mutableStateOf(BodyView.FRONT) }
    
    // Custom Hotspots Database
    val customParts = remember { mutableStateListOf<BodyPart>() }
    
    // Computed list of ALL active parts (Predefined + Custom) matching filter
    val activeParts = remember(selectedSystem, selectedView, customParts.size) {
        val list = PredefinedData.bodyParts.filter { it.system == selectedSystem && it.view == selectedView }
        val customs = customParts.filter { it.system == selectedSystem && it.view == selectedView }
        list + customs
    }

    // Active Study State
    var selectedPart by remember { mutableStateOf<BodyPart?>(null) }
    
    // Ensure selectedPart is valid if system or view changes
    LaunchedEffect(selectedSystem, selectedView) {
        selectedPart = activeParts.firstOrNull()
    }

    // AI Tutor state
    var tutorQuestion by rememberSaveable { mutableStateOf("") }
    var tutorAnswer by remember { mutableStateOf("") }
    var isTutorLoading by remember { mutableStateOf(false) }

    // Quiz States
    var quizType by rememberSaveable { mutableStateOf(QuizType.IDENTIFY_PIN) }
    var isQuizRunning by rememberSaveable { mutableStateOf(false) }
    var quizQuestionsList = remember { mutableStateListOf<BodyPart>() }
    var currentQuestionIndex by rememberSaveable { mutableStateOf(0) }
    var quizCorrectCount by rememberSaveable { mutableStateOf(0) }
    var quizWrongCount by rememberSaveable { mutableStateOf(0) }
    
    // States for IDENTIFY_PIN quiz options
    val quizOptions = remember { mutableStateListOf<String>() }
    var quizSelectedOption by remember { mutableStateOf<String?>(null) }
    var hasAnsweredActiveQuestion by rememberSaveable { mutableStateOf(false) }
    
    // Track correctness highlights on the canvas
    val quizCorrectPartsId = remember { mutableStateOf(setOf<String>()) }
    val quizWrongPartsId = remember { mutableStateOf(setOf<String>()) }
    var quizFeedbackMessage by rememberSaveable { mutableStateOf("") }

    // Points Editor States
    var pendingXPercent by remember { mutableStateOf(0.5f) }
    var pendingYPercent by remember { mutableStateOf(0.5f) }
    var showAddPartDialog by remember { mutableStateOf(false) }
    var newPartName by remember { mutableStateOf("") }
    var newPartDesc by remember { mutableStateOf("") }
    var newPartMnemonic by remember { mutableStateOf("") }
    var newPartFunction by remember { mutableStateOf("") }

    // User Progress and Mastery Database (SRS simulation)
    val progressMap = remember { mutableStateMapOf<String, StudyProgress>() }

    // Helper to generate options
    fun setupIdentifyOptions(correctPart: BodyPart) {
        val alternatives = PredefinedData.bodyParts
            .filter { it.system == selectedSystem && it.id != correctPart.id }
            .shuffled()
            .take(3)
            .map { it.name }
        
        quizOptions.clear()
        quizOptions.addAll((alternatives + correctPart.name).shuffled())
        quizSelectedOption = null
        hasAnsweredActiveQuestion = false
    }

    // Start / Stop Quiz Orchestration
    fun startNewQuiz() {
        if (activeParts.isEmpty()) {
            quizFeedbackMessage = "¡No hay puntos disponibles en este sistema/vista para jugar!"
            return
        }
        isQuizRunning = true
        quizCorrectPartsId.value = emptySet()
        quizWrongPartsId.value = emptySet()
        quizCorrectCount = 0
        quizWrongCount = 0
        currentQuestionIndex = 0
        hasAnsweredActiveQuestion = false
        quizSelectedOption = null
        quizFeedbackMessage = ""
        
        // Shuffle and set questions
        quizQuestionsList.clear()
        val shuffled = activeParts.shuffled().take(8) // Rounds of 8 items
        quizQuestionsList.addAll(shuffled)
        
        // Setup initial options if Identify Pin mode
        if (quizType == QuizType.IDENTIFY_PIN && quizQuestionsList.isNotEmpty()) {
            val correctPart = quizQuestionsList[0]
            setupIdentifyOptions(correctPart)
        }
    }

    // Check answers
    fun submitIdentifyAnswer(selectedAns: String) {
        if (hasAnsweredActiveQuestion || quizQuestionsList.isEmpty()) return
        quizSelectedOption = selectedAns
        hasAnsweredActiveQuestion = true
        
        val correctPart = quizQuestionsList[currentQuestionIndex]
        val isCorrect = selectedAns.equals(correctPart.name, ignoreCase = true)

        val currentProgress = progressMap[correctPart.id] ?: StudyProgress(correctPart.id)
        if (isCorrect) {
            quizCorrectCount++
            quizCorrectPartsId.value = quizCorrectPartsId.value + correctPart.id
            quizFeedbackMessage = "¡Excelente! Has identificado el punto perfectamente."
            progressMap[correctPart.id] = currentProgress.copy(
                timesReviewed = currentProgress.timesReviewed + 1,
                timesCorrect = currentProgress.timesCorrect + 1,
                levelOfMastery = (currentProgress.levelOfMastery + 1).coerceAtMost(5)
            )
        } else {
            quizWrongCount++
            quizWrongPartsId.value = quizWrongPartsId.value + correctPart.id
            quizFeedbackMessage = "Incorrecto. Se trataba de: ${correctPart.name}."
            progressMap[correctPart.id] = currentProgress.copy(
                timesReviewed = currentProgress.timesReviewed + 1,
                timesIncorrect = currentProgress.timesIncorrect + 1,
                levelOfMastery = (currentProgress.levelOfMastery - 1).coerceAtLeast(0)
            )
        }
    }

    fun submitTapAnswer(clickedPart: BodyPart) {
        if (hasAnsweredActiveQuestion || quizQuestionsList.isEmpty()) return
        hasAnsweredActiveQuestion = true
        
        val targetPart = quizQuestionsList[currentQuestionIndex]
        val isCorrect = clickedPart.id == targetPart.id
        
        val currentProgress = progressMap[targetPart.id] ?: StudyProgress(targetPart.id)
        if (isCorrect) {
            quizCorrectCount++
            quizCorrectPartsId.value = quizCorrectPartsId.value + targetPart.id
            quizFeedbackMessage = "¡Fabuloso! Diste en el punto correcto."
            progressMap[targetPart.id] = currentProgress.copy(
                timesReviewed = currentProgress.timesReviewed + 1,
                timesCorrect = currentProgress.timesCorrect + 1,
                levelOfMastery = (currentProgress.levelOfMastery + 1).coerceAtMost(5)
            )
        } else {
            quizWrongCount++
            quizWrongPartsId.value = quizWrongPartsId.value + targetPart.id
            quizFeedbackMessage = "Incorrecto. Eso era: ${clickedPart.name}. El objetivo era: ${targetPart.name}."
            progressMap[targetPart.id] = currentProgress.copy(
                timesReviewed = currentProgress.timesReviewed + 1,
                timesIncorrect = currentProgress.timesIncorrect + 1,
                levelOfMastery = (currentProgress.levelOfMastery - 1).coerceAtLeast(0)
            )
        }
    }

    fun nextQuizQuestion() {
        if (currentQuestionIndex + 1 < quizQuestionsList.size) {
            currentQuestionIndex++
            hasAnsweredActiveQuestion = false
            quizSelectedOption = null
            quizFeedbackMessage = ""
            if (quizType == QuizType.IDENTIFY_PIN) {
                setupIdentifyOptions(quizQuestionsList[currentQuestionIndex])
            }
        } else {
            // End of quiz
            isQuizRunning = false
            quizFeedbackMessage = "¡Felicidades! Completaste el cuestionario. Tu puntuación: $quizCorrectCount/${quizQuestionsList.size}"
        }
    }

    // Query AI Tutor using Gemini Api
    fun askAiTutor(part: BodyPart, question: String) {
        if (question.isBlank()) return
        tutorAnswer = ""
        isTutorLoading = true
        coroutineScope.launch {
            tutorAnswer = GeminiApi.getAnatomyExplanation(
                bodyPartName = part.name,
                system = part.system.name,
                question = question
            )
            isTutorLoading = false
        }
    }

    // Determine layout dynamically based on screen configuration (Side-by-side or stacked)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > 600

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0A0B) // Elegant Dark background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // --- TOP DECORATIVE LOGO AND NAVIGATION ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111114)),
                border = BorderStroke(1.dp, Color(0xFF2A2A2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Logo",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                text = "ANATOMÍA INTERACTIVA",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.5.sp
                            )
                        }
                        
                        // Main mode quick tabs
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val modes = listOf(
                                AppMode.STUDY to "Estudio",
                                AppMode.QUIZ to "Cuestionarios",
                                AppMode.CUSTOM_EDITOR to "Puntos +",
                                AppMode.STATS to "Progreso"
                            )
                            for ((mode, label) in modes) {
                                Button(
                                    onClick = { 
                                        selectedMode = mode 
                                        if (mode == AppMode.QUIZ) isQuizRunning = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedMode == mode) Color(0xFF00E5FF) else Color.Transparent,
                                        contentColor = if (selectedMode == mode) Color(0xFF0A0A0B) else Color(0xFF90A4AE)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Filter Controls: Segment selection for View and System
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // System Picker (Left side)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Sistema: ", fontSize = 12.sp, color = Color(0xFF90A4AE))
                            val systems = listOf(
                                BodySystem.EXTERNAL to "General-Externo",
                                BodySystem.SKELETAL to "Óseo (Esqueleto)",
                                BodySystem.ORGANS to "Órganos Internos"
                            )
                            for ((sys, label) in systems) {
                                FilterChip(
                                    selected = selectedSystem == sys,
                                    onClick = { 
                                        selectedSystem = sys 
                                        // Auto adjust views for Organs mapping
                                        if (sys == BodySystem.ORGANS) {
                                            selectedView = BodyView.ORGANS
                                        } else if (selectedView == BodyView.ORGANS) {
                                            selectedView = BodyView.FRONT
                                        }
                                    },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF1D1D21),
                                        selectedLabelColor = Color(0xFF00E5FF),
                                        containerColor = Color(0xFF111114),
                                        labelColor = Color(0xFF90A4AE)
                                    ),
                                    border = BorderStroke(1.dp, if (selectedSystem == sys) Color(0xFF00E5FF) else Color(0xFF2A2A2E)),
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }

                        // View Selector (Right side)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Vista: ", fontSize = 12.sp, color = Color(0xFF90A4AE))
                            val views = when (selectedSystem) {
                                BodySystem.ORGANS -> listOf(BodyView.ORGANS to "Órganos")
                                else -> listOf(BodyView.FRONT to "Frente", BodyView.BACK to "Espalda")
                            }
                            for ((view, label) in views) {
                                FilterChip(
                                    selected = selectedView == view,
                                    onClick = { selectedView = view },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF1D1D21),
                                        selectedLabelColor = Color(0xFF00E5FF),
                                        containerColor = Color(0xFF111114),
                                        labelColor = Color(0xFF90A4AE)
                                    ),
                                    border = BorderStroke(1.dp, if (selectedView == view) Color(0xFF00E5FF) else Color(0xFF2A2A2E)),
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- MAIN INTERACTIVE WORKSPACE LAYOUT (RESPONSIVE) ---
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    // Left element: Interactive Canvas Box with generous negative margins
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0A0A0B))
                            .border(1.dp, Color(0xFF2A2A2E), RoundedCornerShape(16.dp))
                    ) {
                        BodyCanvas(
                            selectedView = selectedView,
                            selectedSystem = selectedSystem,
                            activeParts = activeParts,
                            selectedPart = selectedPart,
                            quizActivePart = if (isQuizRunning && quizType == QuizType.IDENTIFY_PIN) quizQuestionsList.getOrNull(currentQuestionIndex) else null,
                            quizCorrectPartsId = quizCorrectPartsId.value,
                            quizWrongPartsId = quizWrongPartsId.value,
                            onPartClicked = { part ->
                                if (isQuizRunning) {
                                    if (quizType == QuizType.TAP_TARGET) {
                                        submitTapAnswer(part)
                                    }
                                } else {
                                    selectedPart = part
                                }
                            },
                            onCanvasTappedAtPercent = { x, y ->
                                if (selectedMode == AppMode.CUSTOM_EDITOR) {
                                    pendingXPercent = x
                                    pendingYPercent = y
                                    newPartName = ""
                                    newPartDesc = ""
                                    newPartMnemonic = ""
                                    newPartFunction = ""
                                    showAddPartDialog = true
                                }
                            },
                            editorModeActive = selectedMode == AppMode.CUSTOM_EDITOR
                        )
                        
                        // Overlay guide or instructional label on outer margins
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "ANATOMÍA MESH INTERACTIVA",
                                color = Color(0xFF00E5FF).copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Activos: ${activeParts.size} puntos regulados",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Right element: Tab actions and detail layout panels
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .padding(4.dp)
                    ) {
                        RightFeatureLayout(
                            selectedMode = selectedMode,
                            selectedPart = selectedPart,
                            activeParts = activeParts,
                            customParts = customParts,
                            progressMap = progressMap,
                            quizType = quizType,
                            onQuizTypeChange = { quizType = it },
                            isQuizRunning = isQuizRunning,
                            quizQuestionsList = quizQuestionsList,
                            currentQuestionIndex = currentQuestionIndex,
                            quizCorrectCount = quizCorrectCount,
                            quizWrongCount = quizWrongCount,
                            quizOptions = quizOptions,
                            quizSelectedOption = quizSelectedOption,
                            hasAnsweredActiveQuestion = hasAnsweredActiveQuestion,
                            quizFeedbackMessage = quizFeedbackMessage,
                            onStartQuiz = { startNewQuiz() },
                            onCancelQuiz = { isQuizRunning = false },
                            onSubmitIdentify = { submitIdentifyAnswer(it) },
                            onNextQuizQuestion = { nextQuizQuestion() },
                            onPartSelected = { selectedPart = it },
                            tutorQuestion = tutorQuestion,
                            onTutorQuestionChange = { tutorQuestion = it },
                            tutorAnswer = tutorAnswer,
                            isTutorLoading = isTutorLoading,
                            onAskTutor = { part, question -> askAiTutor(part, question) },
                            onSelectPart = { selectedPart = it }
                        )
                    }
                }
            } else {
                // Vertical Stack layout (Phone or narrow screens)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    // Top element: Interactive Canvas Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0A0A0B))
                            .border(1.dp, Color(0xFF2A2A2E), RoundedCornerShape(16.dp))
                    ) {
                        BodyCanvas(
                            selectedView = selectedView,
                            selectedSystem = selectedSystem,
                            activeParts = activeParts,
                            selectedPart = selectedPart,
                            quizActivePart = if (isQuizRunning && quizType == QuizType.IDENTIFY_PIN) quizQuestionsList.getOrNull(currentQuestionIndex) else null,
                            quizCorrectPartsId = quizCorrectPartsId.value,
                            quizWrongPartsId = quizWrongPartsId.value,
                            onPartClicked = { part ->
                                if (isQuizRunning) {
                                    if (quizType == QuizType.TAP_TARGET) {
                                        submitTapAnswer(part)
                                    }
                                } else {
                                    selectedPart = part
                                }
                            },
                            onCanvasTappedAtPercent = { x, y ->
                                if (selectedMode == AppMode.CUSTOM_EDITOR) {
                                    pendingXPercent = x
                                    pendingYPercent = y
                                    newPartName = ""
                                    newPartDesc = ""
                                    newPartMnemonic = ""
                                    newPartFunction = ""
                                    showAddPartDialog = true
                                }
                            },
                            editorModeActive = selectedMode == AppMode.CUSTOM_EDITOR
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bottom element: Action options panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        RightFeatureLayout(
                            selectedMode = selectedMode,
                            selectedPart = selectedPart,
                            activeParts = activeParts,
                            customParts = customParts,
                            progressMap = progressMap,
                            quizType = quizType,
                            onQuizTypeChange = { quizType = it },
                            isQuizRunning = isQuizRunning,
                            quizQuestionsList = quizQuestionsList,
                            currentQuestionIndex = currentQuestionIndex,
                            quizCorrectCount = quizCorrectCount,
                            quizWrongCount = quizWrongCount,
                            quizOptions = quizOptions,
                            quizSelectedOption = quizSelectedOption,
                            hasAnsweredActiveQuestion = hasAnsweredActiveQuestion,
                            quizFeedbackMessage = quizFeedbackMessage,
                            onStartQuiz = { startNewQuiz() },
                            onCancelQuiz = { isQuizRunning = false },
                            onSubmitIdentify = { submitIdentifyAnswer(it) },
                            onNextQuizQuestion = { nextQuizQuestion() },
                            onPartSelected = { selectedPart = it },
                            tutorQuestion = tutorQuestion,
                            onTutorQuestionChange = { tutorQuestion = it },
                            tutorAnswer = tutorAnswer,
                            isTutorLoading = isTutorLoading,
                            onAskTutor = { part, question -> askAiTutor(part, question) },
                            onSelectPart = { selectedPart = it }
                        )
                    }
                }
            }
        }
    }

    // Modal dialog to configure and insert dynamic hotspots
    if (showAddPartDialog) {
        Dialog(onDismissRequest = { showAddPartDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111114)),
                border = BorderStroke(1.dp, Color(0xFF2A2A2E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "AÑADIR PUNTO PERSONALIZADO",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = "Coordenadas del punto: X: ${(pendingXPercent*100).toInt()}% | Y: ${(pendingYPercent*100).toInt()}%",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = newPartName,
                        onValueChange = { newPartName = it },
                        label = { Text("Nombre del Punto (ej. Bíceps)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF2A2A2E),
                            focusedLabelColor = Color(0xFF00E5FF),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPartDesc,
                        onValueChange = { newPartDesc = it },
                        label = { Text("Descripción corta") },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF2A2A2E),
                            focusedLabelColor = Color(0xFF00E5FF),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPartMnemonic,
                        onValueChange = { newPartMnemonic = it },
                        label = { Text("Truco para memorizar (Mnemotécnica)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF2A2A2E),
                            focusedLabelColor = Color(0xFF00E5FF),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPartFunction,
                        onValueChange = { newPartFunction = it },
                        label = { Text("Función biológica") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF2A2A2E),
                            focusedLabelColor = Color(0xFF00E5FF),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = { showAddPartDialog = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF8A80))
                        ) {
                            Text("Cancelar")
                        }
                        
                        Button(
                            onClick = {
                                if (newPartName.isNotBlank() && newPartDesc.isNotBlank()) {
                                    val part = BodyPart(
                                        name = newPartName,
                                        system = selectedSystem,
                                        view = selectedView,
                                        xPercent = pendingXPercent,
                                        yPercent = pendingYPercent,
                                        description = newPartDesc,
                                        mnemonic = newPartMnemonic,
                                        function = newPartFunction,
                                        isCustom = true
                                    )
                                    customParts.add(part)
                                    selectedPart = part
                                    showAddPartDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color(0xFF041118)
                            )
                        ) {
                            Text("Añadir", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Handles toggling views of Studio Mode, Quiz panel, customization log, and historical statistics.
 */
@Composable
fun RightFeatureLayout(
    selectedMode: AppMode,
    selectedPart: BodyPart?,
    activeParts: List<BodyPart>,
    customParts: List<BodyPart>,
    progressMap: Map<String, StudyProgress>,
    quizType: QuizType,
    onQuizTypeChange: (QuizType) -> Unit,
    isQuizRunning: Boolean,
    quizQuestionsList: List<BodyPart>,
    currentQuestionIndex: Int,
    quizCorrectCount: Int,
    quizWrongCount: Int,
    quizOptions: List<String>,
    quizSelectedOption: String?,
    hasAnsweredActiveQuestion: Boolean,
    quizFeedbackMessage: String,
    onStartQuiz: () -> Unit,
    onCancelQuiz: () -> Unit,
    onSubmitIdentify: (String) -> Unit,
    onNextQuizQuestion: () -> Unit,
    onPartSelected: (BodyPart) -> Unit,
    tutorQuestion: String,
    onTutorQuestionChange: (String) -> Unit,
    tutorAnswer: String,
    isTutorLoading: Boolean,
    onAskTutor: (BodyPart, String) -> Unit,
    onSelectPart: (BodyPart) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111114)),
        border = BorderStroke(1.dp, Color(0xFF2A2A2E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Crossfade(targetState = selectedMode, label = "layoutTransition") { mode ->
            when (mode) {
                AppMode.STUDY -> StudyModeLayout(
                    selectedPart = selectedPart,
                    activeParts = activeParts,
                    onPartSelected = onPartSelected,
                    tutorQuestion = tutorQuestion,
                    onTutorQuestionChange = onTutorQuestionChange,
                    tutorAnswer = tutorAnswer,
                    isTutorLoading = isTutorLoading,
                    onAskTutor = onAskTutor
                )
                
                AppMode.QUIZ -> QuizModeLayout(
                    quizType = quizType,
                    onQuizTypeChange = onQuizTypeChange,
                    isQuizRunning = isQuizRunning,
                    quizQuestionsList = quizQuestionsList,
                    currentQuestionIndex = currentQuestionIndex,
                    quizCorrectCount = quizCorrectCount,
                    quizWrongCount = quizWrongCount,
                    quizOptions = quizOptions,
                    quizSelectedOption = quizSelectedOption,
                    hasAnsweredActiveQuestion = hasAnsweredActiveQuestion,
                    quizFeedbackMessage = quizFeedbackMessage,
                    onStartQuiz = onStartQuiz,
                    onCancelQuiz = onCancelQuiz,
                    onSubmitIdentify = onSubmitIdentify,
                    onNextQuestion = onNextQuizQuestion
                )

                AppMode.CUSTOM_EDITOR -> EditorModeInstructionLayout(
                    activeParts = activeParts,
                    customParts = customParts,
                    onSelectPart = onSelectPart
                )

                AppMode.STATS -> StatsTrackingLayout(
                    activeParts = activeParts,
                    progressMap = progressMap,
                    onSelectPart = onSelectPart
                )
            }
        }
    }
}

/**
 * Detailed information and Artificial Intelligence questioning on a single part.
 */
@Composable
fun StudyModeLayout(
    selectedPart: BodyPart?,
    activeParts: List<BodyPart>,
    onPartSelected: (BodyPart) -> Unit,
    tutorQuestion: String,
    onTutorQuestionChange: (String) -> Unit,
    tutorAnswer: String,
    isTutorLoading: Boolean,
    onAskTutor: (BodyPart, String) -> Unit
) {
    if (selectedPart == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Toca un punto del cuerpo humano en el gráfico para explorarlo.",
                textAlign = TextAlign.Center,
                color = Color(0xFF80DEEA),
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        
        // Target Body Part name & Custom Tag
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selectedPart.name.uppercase(),
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
                if (selectedPart.isCustom) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE040FB).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFE040FB), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Creado por ti", color = Color(0xFFE040FB), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Selector carousel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Navegación: ", fontSize = 11.sp, color = Color(0xFF90A4AE))
                Spacer(modifier = Modifier.width(4.dp))
                LazyRowCustom(activeParts, selectedPart, onPartSelected)
            }
        }

        // Description Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D21)),
                border = BorderStroke(1.dp, Color(0xFF2A2A2E)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "DESCRIPCIÓN ANATÓMICA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedPart.description,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }

        // Physiological Function Box
        if (selectedPart.function.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D21)),
                    border = BorderStroke(1.dp, Color(0xFF2A2A2E)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "FUNCIÓN BIOLÓGICA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22C55E),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedPart.function,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Mnemonic Tricks card
        if (selectedPart.mnemonic.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14241B)),
                    border = BorderStroke(1.dp, Color(0xFF225E33)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star",
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "TRUCO DE MEMORIZACIÓN (MNEMOTÉCNICA)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22C55E),
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedPart.mnemonic,
                            fontSize = 13.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }

        // --- GEMINI TUTOR DIALOG SECTION ---
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Divider(color = Color(0xFF2A2A2E))
            Spacer(modifier = Modifier.height(6.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161618), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF2A2A2E), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "AI",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PROFESOR DE ANATOMÍA INTELIGENTE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                }
                
                Text(
                    text = "Pregúntale al tutor IA sobre el ${selectedPart.name} para enriquecer tu aprendizaje, obtener reglas mnemotécnicas de ayuda, o entender patologías asociadas.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                // Quick prompt selectors to accelerate user question
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val promptSuggestions = listOf(
                        "¿Cuál es su función vital?",
                        "¿Cómo puedo memorizarlo?",
                        "¿Qué pasa si se fractura/lesiona?"
                    )
                    for (suggestion in promptSuggestions) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF1D1D21), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF2A2A2E), RoundedCornerShape(6.dp))
                                .clickable {
                                    onTutorQuestionChange(suggestion)
                                    onAskTutor(selectedPart, suggestion)
                                }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 9.sp,
                                maxLines = 2,
                                color = Color(0xFF90A4AE),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Custom prompt input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tutorQuestion,
                        onValueChange = onTutorQuestionChange,
                        placeholder = { Text("Escribe tu duda aquí...", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF2A2A2E)
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onAskTutor(selectedPart, tutorQuestion) },
                        enabled = tutorQuestion.isNotBlank() && !isTutorLoading,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF0A0A0B),
                            disabledContainerColor = Color(0xFF1D1D21),
                            disabledContentColor = Color(0xFF64748B)
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                    }
                }

                // AI Tutor Output Block
                if (isTutorLoading || tutorAnswer.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1D1D21), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF2A2A2E), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        if (isTutorLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00E5FF))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("El profesor está pensando...", color = Color(0xFF00E5FF), fontSize = 12.sp)
                            }
                        } else {
                            Text(
                                text = tutorAnswer,
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom horizontal navigation items.
 */
@Composable
fun LazyRowCustom(
    list: List<BodyPart>,
    selected: BodyPart?,
    onSelected: (BodyPart) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(list) { item ->
            val isCurrent = selected != null && item.id == selected.id
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isCurrent) Color(0xFF00E5FF) else Color(0xFF1D1D21))
                    .clickable { onSelected(item) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) Color(0xFF0A0A0B) else Color.White
                )
            }
        }
    }
}

/**
 * Practice questioning structures.
 */
@Composable
fun QuizModeLayout(
    quizType: QuizType,
    onQuizTypeChange: (QuizType) -> Unit,
    isQuizRunning: Boolean,
    quizQuestionsList: List<BodyPart>,
    currentQuestionIndex: Int,
    quizCorrectCount: Int,
    quizWrongCount: Int,
    quizOptions: List<String>,
    quizSelectedOption: String?,
    hasAnsweredActiveQuestion: Boolean,
    quizFeedbackMessage: String,
    onStartQuiz: () -> Unit,
    onCancelQuiz: () -> Unit,
    onSubmitIdentify: (String) -> Unit,
    onNextQuestion: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        
        // Main setup view when game is offline
        if (!isQuizRunning) {
            Text(
                text = "ENTRENADOR DE MEMORIA",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF00E5FF),
                letterSpacing = 1.sp
            )
            
            Text(
                text = "Ponte a prueba para confirmar que dominas la ubicación de cada una de las partes de la sección actual.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D21)),
                border = BorderStroke(1.dp, Color(0xFF2A2A2E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selecciona el tipo de examen:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Button Identify pin
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (quizType == QuizType.IDENTIFY_PIN) Color(0xFF151518) else Color(0xFF111114)
                            ),
                            border = BorderStroke(1.5.dp, if (quizType == QuizType.IDENTIFY_PIN) Color(0xFF00E5FF) else Color(0xFF2A2A2E)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onQuizTypeChange(QuizType.IDENTIFY_PIN) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Info, contentDescription = "ID", tint = Color(0xFF00E5FF))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Identificar Punto", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, textAlign = TextAlign.Center)
                                Text("Aparece una luz en el cuerpo y debes elegir su nombre correcto.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                            }
                        }

                        // Button Tap target
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (quizType == QuizType.TAP_TARGET) Color(0xFF151518) else Color(0xFF111114)
                            ),
                            border = BorderStroke(1.5.dp, if (quizType == QuizType.TAP_TARGET) Color(0xFF00E5FF) else Color(0xFF2A2A2E)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onQuizTypeChange(QuizType.TAP_TARGET) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Tap", tint = Color(0xFF00E5FF))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Localizar en Gráfico", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, textAlign = TextAlign.Center)
                                Text("La IA te da un nombre y debes tocar el punto exacto en el cuerpo.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            // Results summary check
            if (quizFeedbackMessage.contains("Completaste")) {
                AlertText(msg = quizFeedbackMessage, success = true)
            } else if (quizFeedbackMessage.isNotEmpty()) {
                AlertText(msg = quizFeedbackMessage, success = false)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onStartQuiz,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color(0xFF0A0A0B)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start")
                    Text("INICIAR EXAMEN (8 PREGUNTAS)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        } else {
            // QUIZ GAME IS ACTIVE
            val activePart = quizQuestionsList.getOrNull(currentQuestionIndex)
            
            if (activePart != null) {
                // Header progress stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PREGUNTA ${currentQuestionIndex + 1} de ${quizQuestionsList.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90A4AE)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✅ $quizCorrectCount", color = Color(0xFF22C55E), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("❌ $quizWrongCount", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                LinearProgressIndicator(
                    progress = (currentQuestionIndex.toFloat() / quizQuestionsList.size.toFloat()),
                    color = Color(0xFF00E5FF),
                    trackColor = Color(0xFF1D1D21),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Render dynamic question cards according to type
                when (quizType) {
                    QuizType.IDENTIFY_PIN -> {
                        Text(
                            text = "¿Cómo se llama la parte señalada por el punto de luz amarillo?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Options vertical panel
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (option in quizOptions) {
                                val isSelected = quizSelectedOption == option
                                val showAnswerAnalysis = hasAnsweredActiveQuestion
                                val isCorrectOption = option == activePart.name

                                val colorBrush = when {
                                    showAnswerAnalysis && isCorrectOption -> Color(0xFF22C55E) // Correct highlights green
                                    showAnswerAnalysis && isSelected -> Color(0xFFEF4444) // User selected wrong highlights red
                                    isSelected -> Color(0xFF00E5FF)
                                    else -> Color(0xFF1D1D21)
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !hasAnsweredActiveQuestion) {
                                            onSubmitIdentify(option)
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected || (showAnswerAnalysis && isCorrectOption)) colorBrush.copy(alpha = 0.15f) else Color(0xFF1D1D21)
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected || (showAnswerAnalysis && isCorrectOption)) 2.dp else 1.dp,
                                        color = if (isSelected || (showAnswerAnalysis && isCorrectOption)) colorBrush else Color(0xFF2A2A2E)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = option, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        
                                        if (showAnswerAnalysis) {
                                            if (isCorrectOption) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = "Correct", tint = Color(0xFF22C55E))
                                            } else if (isSelected) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = "Wrong", tint = Color(0xFFEF4444))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    QuizType.TAP_TARGET -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D21)),
                            border = BorderStroke(1.dp, Color(0xFF2A2A2E)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "LOCALIZA EL PUNTO:",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = activePart.name.uppercase(),
                                    fontSize = 20.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Toca en la silueta corporal el PIN correspondiente a este órgano/hueso.",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Answer Feedback
                if (hasAnsweredActiveQuestion) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (quizFeedbackMessage.contains("Excelente") || quizFeedbackMessage.contains("Fabuloso"))
                                    Color(0xFF22C55E).copy(alpha = 0.12f)
                                  else Color(0xFFEF4444).copy(alpha = 0.12f),
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (quizFeedbackMessage.contains("Excelente") || quizFeedbackMessage.contains("Fabuloso"))
                                    Color(0xFF22C55E).copy(alpha = 0.5f)
                                  else Color(0xFFEF4444).copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = quizFeedbackMessage,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Dato: " + activePart.description,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = onNextQuestion,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("CONTINUAR", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    OutlinedButton(
                        onClick = onCancelQuiz,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                        border = BorderStroke(1.dp, Color(0xFFFF8A80).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("ABANDONAR EXAMEN")
                    }
                }
            }
        }
    }
}

/**
 * Technical helper banner to present feedbacks.
 */
@Composable
fun AlertText(msg: String, success: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (success) Color(0xFF22C55E).copy(alpha = 0.1f) else Color(0xFFEAB308).copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (success) Color(0xFF22C55E) else Color(0xFFEAB308),
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        Text(text = msg, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Editorial interface to create custom reviewed points.
 */
@Composable
fun EditorModeInstructionLayout(
    activeParts: List<BodyPart>,
    customParts: List<BodyPart>,
    onSelectPart: (BodyPart) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "CREADOR DE PUNTOS",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFE040FB),
            letterSpacing = 1.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D21)),
            border = BorderStroke(1.dp, Color(0xFF2A2A2E)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "INSTRUCCIONES:",
                    fontSize = 11.sp,
                    color = Color(0xFFE040FB),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Introduce tus propios focos de memorización para tus exámenes.\n" +
                            "1. Elige el sistema y de qué lado (frente/espalda) arriba.\n" +
                            "2. TOCA cualquier parte vacía en la silueta corporal azul del gráfico.\n" +
                            "3. Introduce el nombre, notas clave, y ¡guárdalo!",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Text(
            text = "TUS PUNTOS PERSONALIZADOS (${customParts.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF90A4AE),
            modifier = Modifier.padding(top = 8.dp)
        )

        if (customParts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, Color(0xFF2A2A2E), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aún no has creado ningún punto.\nToca una zona vacía de la silueta para empezar.",
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillWithMax()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(customParts) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D21)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPart(item) },
                        border = BorderStroke(1.dp, Color(0xFF2A2A2E))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = item.description, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
                            }
                            Icon(imageVector = Icons.Default.Info, contentDescription = "info", tint = Color(0xFFE040FB))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Historical statistics of space-repetition learning index.
 */
@Composable
fun StatsTrackingLayout(
    activeParts: List<BodyPart>,
    progressMap: Map<String, StudyProgress>,
    onSelectPart: (BodyPart) -> Unit
) {
    // Math indicators
    val totalParts = activeParts.size
    val studiedCount = activeParts.count { progressMap.containsKey(it.id) }
    
    val totalCorrect = progressMap.values.sumOf { it.timesCorrect }
    val totalReviews = progressMap.values.sumOf { it.timesReviewed }
    val averageAccuracy = if (totalReviews == 0) 0f else (totalCorrect.toFloat() / totalReviews.toFloat()) * 100f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "PANEL DE PROGRESO",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF00E5FF),
                letterSpacing = 1.sp
            )
        }

        // Dashboard Summary row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Studied Points Box
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D21)),
                    border = BorderStroke(1.dp, Color(0xFF2A2A2E))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Estudiados", fontSize = 11.sp, color = Color(0xFF00E5FF))
                        Text(
                            text = "$studiedCount / $totalParts",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                // Global Accuracy Box
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D21)),
                    border = BorderStroke(1.dp, Color(0xFF2A2A2E))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Precisión global", fontSize = 11.sp, color = Color(0xFF22C55E))
                        Text(
                            text = String.format("%.1f%%", averageAccuracy),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Subtitle listing points progress
        item {
            Text(
                text = "ÍNDICE DE DOMINIO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90A4AE),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (progressMap.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, Color(0xFF2A2A2E), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "El progreso se alimenta de tus respuestas en los cuestionarios.\n¡Realiza un examen para ver estadísticas aquí!",
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            val progressItemsList = activeParts.filter { progressMap.containsKey(it.id) }
            items(progressItemsList) { part ->
                val progress = progressMap[part.id] ?: StudyProgress(part.id)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D21)),
                    border = BorderStroke(1.dp, Color(0xFF2A2A2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPart(part) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = part.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "Respuestas: ${progress.timesReviewed} (Correctas: ${progress.timesCorrect})",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }

                        // Mastery levels tags
                        val masteryLabel = when (progress.levelOfMastery) {
                            in 0..1 -> "Aprendiz"
                            in 2..3 -> "Intermedio"
                            else -> "Experto"
                        }
                        val masteryColor = when (progress.levelOfMastery) {
                            in 0..1 -> Color(0xFFEF4444)
                            in 2..3 -> Color(0xFFEAB308)
                            else -> Color(0xFF22C55E)
                        }

                        Box(
                            modifier = Modifier
                                .background(masteryColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .border(1.dp, masteryColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$masteryLabel (Nivel ${progress.levelOfMastery})",
                                color = masteryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick inline extensions to support size rules logic in Compose.
 */
private fun Modifier.fillWithMax(): Modifier = this.fillMaxWidth()
