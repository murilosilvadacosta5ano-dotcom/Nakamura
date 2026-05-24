package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import com.example.R
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.ChatMessage
import com.example.data.db.ChatSession
import androidx.compose.foundation.text.BasicTextField
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val currentModel by viewModel.currentModel.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    val selectedImageBase64 by viewModel.selectedImageBase64.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()

    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isGuest by viewModel.isGuest.collectAsStateWithLifecycle()
    val currentUserDisplayName by viewModel.currentUserDisplayName.collectAsStateWithLifecycle()

    if (!isLoggedIn && !isGuest) {
        GoogleAuthenticationScreen(
            viewModel = viewModel
        )
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }

    var notebooksExpanded by remember { mutableStateOf(false) }
    var recentsExpanded by remember { mutableStateOf(true) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPersonality by viewModel.selectedPersonality.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var sessionToRename by remember { mutableStateOf<ChatSession?>(null) }
    var renameText by remember { mutableStateOf("") }
    var menuSessionId by remember { mutableStateOf<Long?>(null) }

    // Custom flow states for Bottom Sheet, Canvas, Wikipedia and Image synthesis
    var showPlusSheet by remember { mutableStateOf(false) }
    var showCanvasDialog by remember { mutableStateOf(false) }
    var canvasText by remember { mutableStateOf("") }
    
    var showWikipediaDialog by remember { mutableStateOf(false) }
    var wikipediaQuery by remember { mutableStateOf("") }
    var wikipediaResult by remember { mutableStateOf<String?>(null) }
    var wikipediaLoading by remember { mutableStateOf(false) }

    var showImageGenDialog by remember { mutableStateOf(false) }
    var imageGenPrompt by remember { mutableStateOf("") }
    var isGeneratingImage by remember { mutableStateOf(false) }
    var imageProgress by remember { mutableFloatStateOf(0f) }
    val animatedImageProgress by animateFloatAsState(
        targetValue = imageProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow,
            visibilityThreshold = 1 / 1000f,
        ),
        label = "imageProgress"
    )

    var isTopSearchVisible by remember { mutableStateOf(false) }
    var isDrawerSearchExpanded by remember { mutableStateOf(true) } // Keep it open by default but with a toggle for sliding transition!
    var activeModeExtension by remember { mutableStateOf<String?>(null) }

    var showGoogleLoginDialog by remember { mutableStateOf(false) }
    var showCopyButtonForId by remember { mutableStateOf<Long?>(null) }

    // Colors aligned with Google Gemini brand guidelines
    val geminiDarkBackground = Color(0xFF131314)
    val geminiCardBackground = Color(0xFF1E1F20)
    val geminiAccentBlue = Color(0xFF4285F4)
    val geminiSparkleColors = listOf(
        Color(0xFF4285F4),
        Color(0xFF9B72F4),
        Color(0xFFF072B6)
    )

    // Gallery selector launcher for images
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.attachImage(context, it) }
    }

    // File selector launcher for documents
    val docLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // Do standard mockup logging or toast for doc attachment
    }

    // Handle system back navigation to close open drawer safely
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFF8F9FA),
                drawerTonalElevation = 0.dp,
                modifier = Modifier.width(320.dp),
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                GeminiNavigationDrawer(
                    sessions = sessions,
                    currentSessionId = currentSessionId,
                    isLoggedIn = isLoggedIn,
                    currentUserDisplayName = currentUserDisplayName,
                    isGenerating = isGenerating,
                    onConfiguracoesClick = { showSettingsDialog = true },
                    onNovaConversaClick = {
                        viewModel.startNewChat()
                        coroutineScope.launch { drawerState.close() }
                    },
                    onSessionClick = { sessionId ->
                        viewModel.selectSession(sessionId)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onSessionDelete = { sessionId ->
                        viewModel.deleteSession(sessionId)
                    },
                    onSessionShare = {
                        Toast.makeText(context, "Conversa compartilhada com sucesso!", Toast.LENGTH_SHORT).show()
                    },
                    onSessionPin = {
                        Toast.makeText(context, "Conversa fixada no topo!", Toast.LENGTH_SHORT).show()
                    },
                    onSessionRename = { session ->
                        sessionToRename = session
                        renameText = session.title
                    },
                    onTriggerUpgrade = {
                        showUpgradeDialog = true
                    },
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    },
                    onSuggestionClick = { suggestion ->
                        viewModel.setInputText(suggestion)
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        },
        modifier = modifier
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = geminiDarkBackground
                    ),
                    title = {
                        // Dropdown model selector mimicking original UI image
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showModelMenu = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val readableModelName = "Nano Nakamura Flash"
                                Text(
                                    text = readableModelName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Model options",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showModelMenu,
                                onDismissRequest = { showModelMenu = false },
                                modifier = Modifier.background(geminiCardBackground)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Nano Nakamura Flash", color = Color.White) },
                                    onClick = {
                                        viewModel.setModel("gemini-3.5-flash")
                                        showModelMenu = false
                                    }
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Sidebar drawer toggle",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (isLoggedIn) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4285F4))
                                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                    .clickable { showSettingsDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                val initials = {
                                    val parts = (currentUserDisplayName ?: "Usuário").split(" ")
                                    if (parts.size >= 2) "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                                    else parts[0].take(2).uppercase()
                                }()
                                Text(
                                    text = initials,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        } else {
                            Button(
                                onClick = { showGoogleLoginDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .wrapContentSize()
                            ) {
                                Text(
                                    text = "(login)",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                )
            },
            containerColor = geminiDarkBackground
        ) { innerPadding ->
            val listState = rememberLazyListState()

            // Smoothly auto-scroll to the bottom when messages amount grows or model is generating answers
            LaunchedEffect(messages.size, isGenerating) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Top Search Bar reveal with spring slide-down animation
                AnimatedVisibility(
                    visible = isTopSearchVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(geminiCardBackground)
                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Filtrar conversas...", color = Color.Gray, fontSize = 14.sp)
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setSearchQuery("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Core chat history output sheet
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (currentSessionId == null || messages.isEmpty()) {
                        // Empty Welcome Screen mimicking the user's uploaded image exactly
                        val firstName = if (isLoggedIn) {
                            (currentUserDisplayName ?: "Usuário").split(" ").firstOrNull() ?: "Usuário"
                        } else {
                            "Convidado"
                        }
                        WelcomeSplashScreen(
                            colors = geminiSparkleColors,
                            isGenerating = isGenerating,
                            userName = firstName,
                            onChipClick = { promptText ->
                                viewModel.setInputText(promptText)
                            }
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                val isLatestAiMessage = msg.role != "user" && messages.lastOrNull { it.role != "user" }?.id == msg.id
                                MessageBubble(
                                    message = msg,
                                    colors = geminiSparkleColors,
                                    cardColor = geminiCardBackground,
                                    accentColor = geminiAccentBlue,
                                    isThinking = isGenerating && isLatestAiMessage,
                                    onEditCode = { canvasText = it; showCanvasDialog = true },
                                    showCopyButtonForId = showCopyButtonForId,
                                    onLongPress = { id -> showCopyButtonForId = id },
                                    onDismissCopyButton = { showCopyButtonForId = null },
                                    onLinkAction = { action, url ->
                                        if (action == "ask") {
                                            viewModel.setInputText("Me fale sobre esse site/link: $url")
                                        } else if (action == "search") {
                                            viewModel.setInputText("Pesquisar sobre o link $url")
                                            viewModel.sendMessage(activeModeExtension)
                                        }
                                    }
                                )
                            }

                            if (isGenerating) {
                                item {
                                    GeneratingIndicator(geminiSparkleColors)
                                }
                            }
                        }
                    }
                }

                // Selected attachment preview bar
                AnimatedVisibility(
                    visible = selectedImageUri != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = geminiCardBackground,
                            border = BorderStroke(1.dp, Color.DarkGray)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Load selected bitmap safely
                                val contextResolver = LocalContext.current.contentResolver
                                val bitmap = remember(selectedImageUri) {
                                    try {
                                        selectedImageUri?.let { uri ->
                                            contextResolver.openInputStream(uri).use { stream ->
                                                BitmapFactory.decodeStream(stream)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Selected image preview",
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.DarkGray)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Image attached",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        "Will be submitted as multimodal scan input",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                    )
                                }

                                IconButton(onClick = { viewModel.removeAttachment() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove attachment",
                                        tint = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }

                // BOTTOM INPUT pill layout mimicking prompt page from the image
                if (!showPlusSheet) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    // Active Mode Extension Indicator Pill (e.g. image [x], wikipedia [x], canvas [x])
                    AnimatedVisibility(
                        visible = activeModeExtension != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(start = 58.dp) // Aligns past the floating + button
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2D2E30))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            listOf(Color(0xFFFF8AA4), Color(0xFF8AB4F8))
                                        )
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (activeModeExtension) {
                                    "image" -> Icons.Default.Palette
                                    "wikipedia" -> Icons.Default.Book
                                    "canvas" -> Icons.Default.Build
                                    else -> Icons.Default.Extension
                                },
                                contentDescription = null,
                                tint = Color(0xFFFF8AA4),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${activeModeExtension ?: ""} (x)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { activeModeExtension = null }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Floating circular button separated from the text bar, vertically centered perfectly
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(geminiCardBackground)
                                .border(1.dp, Color(0xFF444746), CircleShape)
                                .clickable { showPlusSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Open Nakamura IA Actions Panel",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color(0xFF444746), RoundedCornerShape(32.dp)),
                            shape = RoundedCornerShape(32.dp),
                            color = geminiCardBackground,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                            // Dynamic multi-line text input field
                            TextField(
                                value = inputText,
                                onValueChange = { viewModel.setInputText(it) },
                                placeholder = {
                                    Text(
                                        text = "Fale com a Nakamura IA...",
                                        color = Color.Gray,
                                        fontSize = 16.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .testTag("ask_gemini_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Send
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (inputText.isNotBlank() || selectedImageBase64 != null) {
                                            viewModel.sendMessage(activeModeExtension)
                                            activeModeExtension = null
                                            keyboardController?.hide()
                                        }
                                    }
                                )
                            )

                            // Send morphing trigger (Microphone when query is blank, Send airplane arrow when typing is ongoing)
                            val isInputEmpty = inputText.isBlank() && selectedImageBase64 == null
                            AnimatedContent(
                                targetState = isInputEmpty,
                                label = "Input morphing action"
                            ) { empty ->
                                if (empty) {
                                    IconButton(
                                        onClick = { showVoiceDialog = true },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Mic,
                                            contentDescription = "Simulated listening prompt dictation",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            viewModel.sendMessage(activeModeExtension)
                                            activeModeExtension = null
                                            keyboardController?.hide()
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF004A77))
                                            .testTag("submit_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send prompt",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive Voice Dialog overlay
    if (showVoiceDialog) {
        VoiceListeningDialog(
            onDismiss = { showVoiceDialog = false },
            onTextSelect = { selectedPrompt ->
                viewModel.setInputText(selectedPrompt)
                showVoiceDialog = false
            }
        )
    }

    // Modern Settings & Personality Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            selectedPersonality = selectedPersonality,
            onPersonalitySelect = { viewModel.setPersonality(it) },
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false },
            onTriggerUpgrade = {
                showSettingsDialog = false
                showUpgradeDialog = true
            }
        )
    }

    // Google Gemini Upgrade Advanced Dialog
    if (showUpgradeDialog) {
        UpgradeAdvancedDialog(
            onDismiss = { showUpgradeDialog = false }
        )
    }

    // Edit recent session title Dialog
    sessionToRename?.let { session ->
        RenameSessionDialog(
            session = session,
            currentText = renameText,
            onValueChange = { renameText = it },
            onSave = {
                if (renameText.isNotBlank()) {
                    viewModel.renameSession(session.id, renameText)
                }
                sessionToRename = null
            },
            onDismiss = { sessionToRename = null }
        )
    }

    if (showCanvasDialog) {
        CanvasDialog(
            code = canvasText,
            onCodeChange = { canvasText = it },
            onDismiss = { showCanvasDialog = false }
        )
    }

    NakamuraPlusSheet(
        visible = showPlusSheet,
        onDismiss = { showPlusSheet = false },
        activeModeExtension = activeModeExtension,
        onModeSelect = { activeModeExtension = it },
        showCanvasDialog = { showCanvasDialog = true },
        showSettingsDialog = { showSettingsDialog = true },
        galleryLauncher = galleryLauncher,
        docLauncher = docLauncher,
        geminiCardBackground = geminiCardBackground
    )
}
}

// Nakamura IA custom logo icon from local resource with animated rotating ring in thinking state
@Composable
fun SparkleIcon(
    size: Int = 40,
    colors: List<Color> = emptyList(),
    modifier: Modifier = Modifier,
    isThinking: Boolean = false
) {
    // Rotation state: when thinking, rotate the ring. Otherwise stop rotating.
    val infiniteTransition = rememberInfiniteTransition(label = "Ring rotation")
    val rotationAngle by if (isThinking) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Rotation Angle"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Outer container layout
    Box(
        modifier = modifier
            .size((size * 1.51f).dp), // Extra space for the outer ring to be clear of borders
        contentAlignment = Alignment.Center
    ) {
        // 1. The rotating outer ring ("um anel em volta branco" + azul com branco e meio verde)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(rotationZ = rotationAngle)
                .padding(2.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = (size / 15f).coerceAtLeast(1.5f).dp.toPx()
                val r = (size * 0.60f).dp.toPx()
                val arcSize = Size(r * 2, r * 2)
                val arcTopLeft = Offset(center.x - r, center.y - r)
                
                // Draw the main white ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = r,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f) // Sleek dashed design
                    )
                )

                // Draw secondary blue and green accent dots/arcs on the ring to match "azul com branco e verde"
                drawArc(
                    color = Color(0xFF1E88E5), // Blue accent
                    startAngle = 45f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth + 1.dp.toPx())
                )

                drawArc(
                    color = Color(0xFF00E676), // Green accent
                    startAngle = 225f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth + 1.dp.toPx())
                )
            }
        }

        // 2. Central Core Icon with adaptive white borders/background
        // This ensures high contrast against any background color ("tipo as bordas branca do ícone")
        Box(
            modifier = Modifier
                .size(size.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape) // Adaptive background with white color
                .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape) // White border
                .padding(1.dp), // Tiny padding to show the white board cleanly
            contentAlignment = Alignment.Center
        ) {
            coil.compose.AsyncImage(
                model = R.drawable.nakamura_logo,
                contentDescription = "Nakamura IA Icon",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )
        }
    }
}

data class SuggestionChipItem(
    val text: String,
    val icon: ImageVector,
    val iconColor: Color
)

@Composable
fun SuggestionChipCard(
    item: SuggestionChipItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E1F20),
        border = BorderStroke(1.dp, Color(0xFF444746))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.iconColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE3E3E3),
                    lineHeight = 16.sp
                ),
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Welcome Splash Screen centered visual matching the prompt picture
@Composable
fun WelcomeSplashScreen(
    colors: List<Color>,
    isGenerating: Boolean = false,
    userName: String = "Convidado",
    onChipClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val sampleBubbles = listOf(
                "girl what...",
                "no like what do I say",
                "just one answer",
                "I farted so hard everyone's unconscious",
                "I'm deporting you back to the Apple Store"
            )
            sampleBubbles.forEach { text ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .clickable { onChipClick(text) }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = text,
                        color = Color(0xFF1E1E1E),
                        style = TextStyle(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Hello!, $userName",
                style = TextStyle(
                    fontFamily = CherryBombFontFamily,
                    fontSize = 28.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            )

            Text(
                text = "Your chat style",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            )

            Text(
                text = "Playful, curious, and expressive with bursts of humor, quick reactions, and a mix of slang and candid emotional honesty in every message.",
                style = TextStyle(
                    fontSize = 14.5.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}// Custom code-formatting & simple parser inside standard chat bubbles
@Composable
fun MessageBubble(
    message: ChatMessage,
    colors: List<Color>,
    cardColor: Color,
    accentColor: Color,
    isThinking: Boolean = false,
    onEditCode: (String) -> Unit = {},
    showCopyButtonForId: Long? = null,
    onLongPress: (Long) -> Unit = {},
    onDismissCopyButton: () -> Unit = {},
    onLinkAction: (String, String) -> Unit = { _, _ -> }
) {
    val isUser = message.role == "user"
    val isError = message.role == "error"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 1.0f)
        ) {
            if (!isUser) {
                // Assist avatar Sparkle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(cardColor),
                    contentAlignment = Alignment.Center
                ) {
                    SparkleIcon(size = 18, colors = colors, isThinking = isThinking)
                }
            }

            Column(
                modifier = Modifier.weight(1.0f),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                if (isUser) {
                    // Render base64 image scan thumbnails for user multimodal inputs
                    if (!message.imageBase64.isNullOrEmpty()) {
                        val bitmap = remember(message.imageBase64) {
                            try {
                                if (message.imageBase64.startsWith("http")) {
                                    null
                                } else {
                                    val bytes = android.util.Base64.decode(message.imageBase64, android.util.Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Scanned Attachment",
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else if (message.imageBase64.startsWith("http")) {
                            // Support loading generated HTTP URLs inside images
                            coil.compose.AsyncImage(
                                model = message.imageBase64,
                                contentDescription = "Generated Image output",
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        BorderStroke(
                                            1.5.dp,
                                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                listOf(Color(0xFFFF8AA4), Color(0xFF8AB4F8))
                                            )
                                        ),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // User text container bubble with long-press copy support
                    if (message.content.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = 20.dp,
                                bottomEnd = 4.dp
                            ),
                            color = cardColor,
                            modifier = Modifier
                                .border(1.dp, Color.DarkGray, RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = 20.dp,
                                    bottomEnd = 4.dp
                                ))
                                .pointerInput(message.id) {
                                    detectTapGestures(
                                        onLongPress = { onLongPress(message.id) }
                                    )
                                }
                        ) {
                            Text(
                                text = message.content,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                } else if (isError) {
                    // System configuration errors or network errors bubble layout style
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF3C1F1F),
                        border = BorderStroke(1.dp, Color(0xFFCA3A3A))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Configuration Notice",
                                tint = Color(0xFFE57373)
                            )
                            Text(
                                text = "hm, houve um erro no banco de dados espere e tente mais tarde",
                                color = Color(0xFFFFD2D2),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    // AI Response card with long-press copy support
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier.pointerInput(message.id) {
                            detectTapGestures(
                                onLongPress = { onLongPress(message.id) }
                            )
                        }
                    ) {
                        // Support rendering HTTP output link for generated images inline
                        if (message.content.startsWith("https://image.pollinations.ai")) {
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                coil.compose.AsyncImage(
                                    model = message.content,
                                    contentDescription = "AI Generated Image",
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(280.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(
                                            BorderStroke(
                                                2.dp,
                                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                    listOf(Color(0xFFFF8AA4), Color(0xFF8AB4F8))
                                                )
                                            ),
                                            RoundedCornerShape(20.dp)
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = "Aqui está a sua imagem gerada por Nakamura IA com Pollinations!",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            val parsed = remember(message.content) { parseMsgWithCode(message.content) }
                            Column {
                                if (parsed.second != null) {
                                    AiMessageCard(
                                        message = parsed.first,
                                        code = parsed.second,
                                        language = parsed.third,
                                        onEditCode = onEditCode
                                    )
                                } else {
                                    // Nakamura formatting parser to handle block markdown layout gracefully
                                    MarkdownContent(
                                        text = message.content,
                                        cardColor = cardColor,
                                        onEditCode = onEditCode,
                                        onLinkAction = onLinkAction
                                    )
                                }

                                // If the AI searched some sites, display them styled with border bars at the bottom
                                val simulatedSites = remember(message.content) {
                                    if (message.content.lowercase().contains("wikipedia") || message.content.lowercase().contains("wikipédia")) {
                                        listOf("https://pt.wikipedia.org/wiki/Google", "https://github.com/google/fonts")
                                    } else if (message.content.length > 40) {
                                        listOf("https://github.com/google/fonts", "https://stackoverflow.com/questions/tagged/kotlin")
                                    } else {
                                        emptyList()
                                    }
                                }
                                if (simulatedSites.isNotEmpty()) {
                                    ResearchedSitesRow(sites = simulatedSites)
                                }
                            }
                        }
                    }
                }

                // Smooth animated floating copy button under bubble
                AnimatedVisibility(
                    visible = showCopyButtonForId == message.id,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(message.content))
                            Toast.makeText(context, "Texto copiado!", Toast.LENGTH_SHORT).show()
                            onDismissCopyButton()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(top = 4.dp).align(if (isUser) Alignment.End else Alignment.Start)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Icon",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Copy text", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// Typing dynamic simulation with customized quick/slow speed
@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    onLinkAction: (String, String) -> Unit = { _, _ -> }
) {
    var visibleText by remember(text) { mutableStateOf("") }
    val speed = remember(text) {
        if (text.length > 500) 4L
        else if (text.length > 200) 10L
        else 24L
    }
    LaunchedEffect(text) {
        for (i in 1..text.length) {
            visibleText = text.substring(0, i)
            kotlinx.coroutines.delay(speed)
        }
    }
    LinkifiedText(text = visibleText, modifier = modifier, onLinkAction = onLinkAction)
}

// Glowing Pink-and-Blue gradient capsule borders layout for accessible HTTP links
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LinkifiedText(
    text: String,
    modifier: Modifier = Modifier,
    onLinkAction: (String, String) -> Unit = { _, _ -> }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val urlPattern = remember {
        java.util.regex.Pattern.compile(
            "(https?://[\\w-]+(\\.[\\w-]+)+[\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])"
        )
    }
    val matcher = remember(text) { urlPattern.matcher(text) }

    var heldLinkUrl by remember { mutableStateOf<String?>(null) }

    // Check if the current paragraph mimics a custom title text layout (starts with `#`, `**` or represents title blocks)
    val isTitle = text.trim().startsWith("#") || text.trim().startsWith("**") || text.trim().startsWith("Título:") || text.trim().startsWith("Titulo:")
    val cleanTextDisplay = text.replace("#", "").replace("**", "")

    val textStyle = if (isTitle) {
        TextStyle(
            fontFamily = CherryBombFontFamily,
            fontSize = 20.sp,
            color = Color.White,
            lineHeight = 26.sp
        )
    } else {
        MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
    }

    if (!matcher.find()) {
        Text(
            text = cleanTextDisplay,
            color = Color.White,
            style = textStyle,
            modifier = modifier
        )
        return
    }

    matcher.reset()

    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        var lastIndex = 0
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            if (start > lastIndex) {
                Text(
                    text = text.substring(lastIndex, start).replace("#", "").replace("**", ""),
                    color = Color.White,
                    style = textStyle
                )
            }

            val url = text.substring(start, end)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (heldLinkUrl == url) Color(0xFF1E1F20) else Color(0x22FF2A54)
                        )
                        .pointerInput(url) {
                            detectTapGestures(
                                onLongPress = {
                                    heldLinkUrl = url
                                },
                                onTap = {
                                    try {
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {}
                                }
                            )
                        }
                        .border(
                            BorderStroke(
                                if (heldLinkUrl == url) 2.5.dp else 1.dp,
                                if (heldLinkUrl == url) {
                                    androidx.compose.ui.graphics.Brush.sweepGradient(
                                        listOf(Color(0xFF4285F4), Color(0xFF9B72F4), Color(0xFFFF8AA4), Color(0xFF4285F4))
                                    )
                                } else {
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(Color(0xFFFF8AA4), Color(0xFF8AB4F8))
                                    )
                                }
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Link",
                            tint = Color(0xFFFF8AA4),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = url,
                            color = Color(0xFF8AB4F8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )
                    }
                }

                // Drop-down options overlay for held links (copy link, ask link, search link)
                AnimatedVisibility(
                    visible = heldLinkUrl == url,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 4.dp)
                            .background(Color(0xFF131314), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(url))
                                android.widget.Toast.makeText(context, "Link copiado!", android.widget.Toast.LENGTH_SHORT).show()
                                heldLinkUrl = null
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222428)),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("copy link", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onLinkAction("ask", url)
                                heldLinkUrl = null
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222428)),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("ask link", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onLinkAction("search", url)
                                heldLinkUrl = null
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222428)),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("search link", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            lastIndex = end
        }
        if (lastIndex < text.length) {
            Text(
                text = text.substring(lastIndex).replace("#", "").replace("**", ""),
                color = Color.White,
                style = textStyle
            )
        }
    }
}

// Formats basic Markdown structures (like Code Snippet blocks) elegantly
@Composable
fun MarkdownContent(
    text: String,
    cardColor: Color,
    onEditCode: (String) -> Unit = {},
    onLinkAction: (String, String) -> Unit = { _, _ -> }
) {
    val blocks = remember(text) { splitTextByCodeBlocks(text) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        for (block in blocks) {
            if (block.isCode) {
                // Code block formatted card with monospace typeface and scrollable logic
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, Color.DarkGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = block.language.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Cyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                val context = androidx.compose.ui.platform.LocalContext.current

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(block.content))
                                        android.widget.Toast.makeText(context, "Código copiado!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar Código",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        android.widget.Toast.makeText(context, "Código salvo em Downloads!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download Código",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        onEditCode(block.content)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar no Canvas",
                                        tint = Color(0xFFFF8AA4),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = block.content,
                            color = Color(0xFFA9B7C6),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // Typewriter animated paragraph
                TypewriterText(
                    text = block.content,
                    modifier = Modifier.fillMaxWidth(),
                    onLinkAction = onLinkAction
                )
            }
        }
    }
}

data class CodeBlockSegment(
    val content: String,
    val isCode: Boolean,
    val language: String = "code"
)

private fun splitTextByCodeBlocks(text: String): List<CodeBlockSegment> {
    if (!text.contains("```")) {
        return listOf(CodeBlockSegment(text, false))
    }

    val segments = mutableListOf<CodeBlockSegment>()
    val parts = text.split("```")
    for (i in parts.indices) {
        val part = parts[i]
        if (i % 2 == 1) {
            // Found a markdown code block segment
            val lines = part.split("\n", limit = 2)
            val lang = if (lines.isNotEmpty() && lines[0].trim().length < 15) lines[0].trim() else "code"
            val codeBody = if (lines.size > 1) lines[1] else lines[0]
            segments.add(
                CodeBlockSegment(
                    content = codeBody.trimEnd(),
                    isCode = true,
                    language = if (lang.isEmpty()) "code" else lang
                )
            )
        } else {
            if (part.isNotEmpty()) {
                segments.add(CodeBlockSegment(part, false))
            }
        }
    }
    return segments
}

// Gorgeous animated generative loader with Material 3 LoadingIndicator
@Composable
fun GeneratingIndicator(colors: List<Color>) {
    var currentPhase by remember { mutableStateOf("Pensando...") }
    var showSitesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        currentPhase = "Pensando..."
        kotlinx.coroutines.delay(2500)
        currentPhase = "Buscando nos sites..."
    }

    val simulatedSites = listOf(
        "https://pt.wikipedia.org/wiki/Google",
        "https://github.com/google/fonts",
        "https://stackoverflow.com/questions/tagged/kotlin"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "border_glow")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 44.dp, top = 8.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(
                BorderStroke(
                    1.5.dp,
                    Color.White.copy(alpha = alphaAnim) // Faint weak light edge glow
                ),
                RoundedCornerShape(12.dp)
            )
            .clickable { showSitesDialog = true }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dots_pulsing"
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(CircleShape)
                .background(Color(0xFF4285F4))
        )
        Text(
            text = currentPhase,
            color = Color.White,
            style = TextStyle(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }

    if (showSitesDialog) {
        Dialog(onDismissRequest = { showSitesDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1F20),
                border = BorderStroke(1.dp, Color.DarkGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Fontes lidas por Nakamura IA:",
                        style = TextStyle(
                            fontFamily = CherryBombFontFamily,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "A inteligência artificial Nakamura está processando as seguintes páginas da web para responder à sua dúvida:",
                        style = TextStyle(fontSize = 12.sp, color = Color.Gray)
                    )
                    HorizontalDivider(color = Color.DarkGray)
                    
                    simulatedSites.forEach { site ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = site,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                color = Color(0xFF4285F4),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lido ✔",
                                color = Color(0xFF43B581),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Button(
                        onClick = { showSitesDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Fechar", color = Color.White)
                    }
                }
            }
        }
    }
}

// Simulated dynamic dictation popup with animated canvas-drawn sine ripples
@Composable
fun VoiceListeningDialog(
    onDismiss: () -> Unit,
    onTextSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1E1F20),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Listening...",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                // Render dynamic canvas animated sound Waves
                VoiceWaveformAnimation()

                Text(
                    text = "Try asking about any of these:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )

                val suggestions = listOf(
                    "Draft an email about the project launch",
                    "Explain the concept of neural networks",
                    "Help me plan a 3-day itinerary for Tokyo",
                    "Write a joke about programming"
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (tip in suggestions) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onTextSelect(tip) },
                            color = Color.Black.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = tip,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.LightGray,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }

                TextButton(onClick = { onDismiss() }) {
                    Text("Cancel", color = Color(0xFF4285F4))
                }
            }
        }
    }
}

// Pure sine wave canvas-drawn ripples for interactive voice waveforms
@Composable
fun VoiceWaveformAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave phase loop")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val heightGlow by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amp animate"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val path1 = Path()
        val path2 = Path()

        path1.moveTo(0f, centerY)
        path2.moveTo(0f, centerY)

        for (x in 0..width.toInt() step 5) {
            val normalizedX = x / width
            // Shape the bounding envelope to taper waves beautiful at both screen borders
            val envelope = sin(normalizedX * Math.PI).toFloat()

            val y1 = centerY + (heightGlow * envelope * sin((normalizedX * 4 * Math.PI) + phase)).toFloat()
            val y2 = centerY + ((heightGlow * 0.7f) * envelope * sin((normalizedX * 5.5 * Math.PI) - phase + Math.PI/2)).toFloat()

            path1.lineTo(x.toFloat(), y1)
            path2.lineTo(x.toFloat(), y2)
        }

        drawPath(
            path = path1,
            color = Color(0xFF4285F4),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = path2,
            color = Color(0xFF9B72F4),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// ======================= CUSTOM DIALOG OVERLAYS =======================
@Composable
fun SettingsDialog(
    selectedPersonality: String,
    onPersonalitySelect: (String) -> Unit,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onTriggerUpgrade: () -> Unit
) {
    var currentSection by remember { mutableStateOf<String?>(null) }
    val importMemoryEnabled by viewModel.importMemoryEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F9)), // Authentic light slate grey
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentSection != null) {
                            IconButton(onClick = { currentSection = null }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint = Color(0xFF1F1F1F),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = Color(0xFF1F1F1F),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = when (currentSection) {
                                "activity" -> "Atividade de Conversa"
                                "personalidade" -> "Inteligência Pessoal"
                                "memoria" -> "Importar Memória"
                                "nsfw" -> "Limites de Uso & NSFW"
                                "gems" -> "Gems Customizados"
                                "termos" -> "Termos e Políticas"
                                "tema" -> "Configuração de Tema"
                                "assinaturas" -> "Assinaturas Ativas"
                                "notebooklm" -> "Workspace NotebookLM"
                                "sobre_feedback" -> "Sobre & Feedback"
                                "help" -> "Central de Ajuda"
                                else -> "Configurações do Gemini"
                            },
                            style = TextStyle(
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F1F1F)
                            )
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color(0xFF444746),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Scrollable Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp, max = 450.dp)
                ) {
                    if (currentSection == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Part 1: Main Config Group in a unified white card
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Row 1: Atividade
                                    SettingsRow(
                                        icon = Icons.Outlined.History,
                                        title = "Atividade de Conversa"
                                    ) {
                                        currentSection = "activity"
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F3F4), thickness = 1.dp)

                                    // Row 2: Inteligência Pessoal
                                    SettingsRow(
                                        icon = Icons.Outlined.Psychology,
                                        title = "Inteligência de Personalidade",
                                        trailingContent = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = selectedPersonality,
                                                    style = TextStyle(fontSize = 12.sp, color = Color.Gray)
                                                )
                                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    ) {
                                        currentSection = "personalidade"
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F3F4), thickness = 1.dp)

                                    // Row 3: Importar memória para o Gemini
                                    SettingsRow(
                                        icon = Icons.Outlined.CloudUpload,
                                        title = "Importar memória para o Gemini",
                                        trailingContent = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    color = Color(0xFF1A73E8),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        text = "New",
                                                        color = Color.White,
                                                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    ) {
                                        currentSection = "memoria"
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F3F4), thickness = 1.dp)

                                    // Row 4: Limites de Uso
                                    SettingsRow(
                                        icon = Icons.Outlined.DonutLarge,
                                        title = "Limites de Uso & NSFW",
                                        trailingContent = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("Beta", style = TextStyle(fontSize = 11.sp, color = Color(0xFFD93025), fontWeight = FontWeight.Bold))
                                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    ) {
                                        currentSection = "nsfw"
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F3F4), thickness = 1.dp)

                                    // Row 5: Gems Customizados
                                    SettingsRow(
                                        icon = Icons.Outlined.Diamond,
                                        title = "Gems Customizados"
                                    ) {
                                        currentSection = "gems"
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F3F4), thickness = 1.dp)

                                    // Row 6: Links Públicos
                                    SettingsRow(
                                        icon = Icons.Outlined.Link,
                                        title = "Seus links públicos"
                                    ) {
                                        currentSection = "termos"
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F3F4), thickness = 1.dp)

                                    // Row 7: Tema
                                    SettingsRow(
                                        icon = Icons.Outlined.WbSunny,
                                        title = "Tema Visual",
                                        trailingContent = {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    ) {
                                        currentSection = "tema"
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F3F4), thickness = 1.dp)

                                    // Row 8: Ver Assinaturas
                                    SettingsRow(
                                        icon = Icons.Outlined.CreditCard,
                                        title = "Ver assinaturas"
                                    ) {
                                        currentSection = "assinaturas"
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F3F4), thickness = 1.dp)

                                    // Row 9: Upgrade Advanced
                                    SettingsRow(
                                        icon = Icons.Outlined.AutoAwesome,
                                        title = "Upgrade para Google AI Plus",
                                        trailingContent = {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = Color(0xFF1E88E5),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    ) {
                                        onTriggerUpgrade()
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F3F4), thickness = 1.dp)

                                    // Row 10: NotebookLM
                                    SettingsRow(
                                        icon = Icons.Outlined.WorkspacePremium,
                                        title = "Estudo NotebookLM em Workspace"
                                    ) {
                                        currentSection = "notebooklm"
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F3F4), thickness = 1.dp)

                                    // Row 11: Feedback e Sobre
                                    SettingsRow(
                                        icon = Icons.Outlined.Feedback,
                                        title = "Sobre o App e Feedback"
                                    ) {
                                        currentSection = "sobre_feedback"
                                    }
                                }
                            }

                            // Part 2: Help Group in a single white card
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SettingsRow(
                                    icon = Icons.Outlined.HelpOutline,
                                    title = "Ajuda e Diretrizes"
                                ) {
                                    currentSection = "help"
                                }
                            }

                            // Part 3: Account Sign Out Group
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SettingsRow(
                                    icon = Icons.Default.ExitToApp,
                                    title = "Sair da Conta (Logout)",
                                    iconTint = Color(0xFFD93025),
                                    titleColor = Color(0xFFD93025)
                                ) {
                                    viewModel.logout()
                                    onDismiss()
                                }
                            }
                        }
                    } else {
                        // Subscreen Content Pane
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            when (currentSection) {
                                "activity" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Histórico de Atividade Local",
                                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                                        )
                                        Text(
                                            text = "Suas conversas antigas estão seguras localmente em seu dispositivo celular e são utilizadas como base de memória quando ativada.",
                                            style = TextStyle(fontSize = 12.5.sp, color = Color(0xFF444746)),
                                            lineHeight = 18.sp
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                viewModel.clearAllHistory()
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD93025)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                                                Text("Limpar todo o Histórico completo", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Text(
                                            text = "Atenção: Esta ação é permanente e irreversível. Limpar o histórico apagará do banco de dados SQLite todas as sessões anteriores instantaneamente.",
                                            style = TextStyle(fontSize = 10.5.sp, color = Color.Gray),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                "personalidade" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "Escolha a personalidade ativa do Gemini:",
                                            style = TextStyle(fontSize = 13.5.sp, color = Color(0xFF444746), fontWeight = FontWeight.Medium)
                                        )
                                        val options = listOf("IA", "Modo História", "Agente", "Personagem", "Professor", "Interpretador")
                                        options.forEach { option ->
                                            val isSelected = option == selectedPersonality
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) Color(0xFFE8F0FE) else Color.White)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) Color(0xFF1A73E8) else Color(0xFFE0E0E0),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { onPersonalitySelect(option) }
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = option,
                                                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                                                    )
                                                    val desc = when (option) {
                                                        "IA" -> "Assistente amigável, acolhedor, rápido e objetivo."
                                                        "Modo História" -> "Contador literário de histórias, romance dramático e fantasia."
                                                        "Agente" -> "Foco em resultados operativos, etapas passo a passo lógicas."
                                                        "Personagem" -> "Companheiro expressivo interpretando sentimentos entre asteriscos."
                                                        "Professor" -> "Acadêmico, didático e empático com analogias didáticas."
                                                        "Interpretador" -> "Analista literário de semânticas e entrelinhas textuais."
                                                        else -> ""
                                                    }
                                                    Text(
                                                        text = desc,
                                                        style = TextStyle(fontSize = 11.5.sp, color = Color.DarkGray)
                                                    )
                                                }
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { onPersonalitySelect(option) },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = Color(0xFF1A73E8),
                                                        unselectedColor = Color.Gray
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                "memoria" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Inteligência de Memória Contextual",
                                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                                        )
                                        Text(
                                            text = "Com essa opção ativada, a IA passa a ler temporariamente suas conversas antigas de outras abas para que a resposta atual seja incrivelmente personalizada, entendendo seus gostos, preferências e assuntos tratados anteriormente.",
                                            style = TextStyle(fontSize = 12.5.sp, color = Color(0xFF444746)),
                                            lineHeight = 18.sp
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White, RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Habilitar Retenção de Memória",
                                                    style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                                                )
                                                Text(
                                                    text = if (importMemoryEnabled) "Ativo - Analisando chats anteriores" else "Inativo - Isolamento de chats",
                                                    style = TextStyle(fontSize = 11.sp, color = if (importMemoryEnabled) Color(0xFF1E88E5) else Color.Gray)
                                                )
                                            }
                                            Switch(
                                                checked = importMemoryEnabled,
                                                onCheckedChange = { viewModel.setImportMemoryEnabled(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = Color(0xFF1A73E8)
                                                )
                                            )
                                        }
                                    }
                                }

                                "nsfw" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Limites de Segurança & Filtros",
                                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                                        )
                                        Text(
                                            text = "Ajuste os filtros de decência. Habilitar beijos apaixonados e conflitos literários intensificados em dramatizações.",
                                            style = TextStyle(fontSize = 12.5.sp, color = Color(0xFF444746)),
                                            lineHeight = 18.sp
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White, RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Falar NSFW (Beta)",
                                                    style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                                )
                                                Text(
                                                    text = "Filtro livre de dramatização de histórias (Bloqueado nesta versão preliminar)",
                                                    style = TextStyle(fontSize = 11.sp, color = Color.Gray)
                                                )
                                            }
                                            Switch(
                                                checked = false,
                                                onCheckedChange = null,
                                                enabled = false
                                            )
                                        }
                                        Text(
                                            text = "Opção desativada e em versão beta preventiva para assegurar as diretrizes de integridade da API.",
                                            style = TextStyle(fontSize = 10.5.sp, color = Color.Gray)
                                        )
                                    }
                                }

                                "gems" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Meus Gems Customizados",
                                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                                        )
                                        Text(
                                            text = "Defina pequenos moldes de robôs especializados para tarefas de codificação, tradução rápida ou acompanhamento emocional rápido.",
                                            style = TextStyle(fontSize = 12.5.sp, color = Color(0xFF444746))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White, RoundedCornerShape(12.dp))
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Sincronize recursos de Gems no menu Advanced.", color = Color.Gray, fontSize = 12.sp)
                                        }
                                    }
                                }

                                "termos" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Termos e Políticas de Privacidade",
                                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                                        )
                                        Text(
                                            text = "Segurança dos Dados do Usuário:\nTodo o processamento gerado localmente em seu aparelho de celular é privado. As chaves de seguranças fornecidas em Secrets agem de modo direto via requisições HTTPS para servidores seguros da API.",
                                            style = TextStyle(fontSize = 12.sp, color = Color.DarkGray),
                                            lineHeight = 17.sp
                                        )
                                        Text(
                                            text = "Termos de Uso:\nO uso de inteligência artificial generativa é estritamente pessoal, focado em aprendizado escolar, códigos programáticos e dramatização lúdica.",
                                            style = TextStyle(fontSize = 12.sp, color = Color.DarkGray),
                                            lineHeight = 17.sp
                                        )
                                    }
                                }

                                "tema" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Escolha o Estilo do Tema",
                                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                                        )
                                        Text(
                                            text = "Ajuste o visual geral do aplicativo.",
                                            style = TextStyle(fontSize = 12.5.sp, color = Color(0xFF444746))
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Button(
                                                onClick = {},
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131314)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Escuro (Ativo)", color = Color.White)
                                            }
                                            OutlinedButton(
                                                onClick = {},
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Claro", color = Color.Black)
                                            }
                                        }
                                    }
                                }

                                "assinaturas" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Seu Plano Atual",
                                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                                        )
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("Gemini Free Tier", fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F), fontSize = 14.sp)
                                                Text("Você está utilizando a chave de API padrão gratuita com limite padrão de chamadas.", fontSize = 12.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }

                                "notebooklm" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "NotebookLM Workspace",
                                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                                        )
                                        Text(
                                            text = "Permite carregar múltiplos materiais de estudos densos, PDFs, e de forma automática produzir resumos completos ou dublagens em podcasts de estudo.",
                                            style = TextStyle(fontSize = 12.5.sp, color = Color(0xFF444746)),
                                            lineHeight = 18.sp
                                        )
                                    }
                                }

                                "sobre_feedback" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Sobre o Gemini AI Workspace",
                                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                                        )
                                        Text(
                                            text = "Versão: v2.2.0-beta\nCódigo de Compilação: 104\nDesenvolvido com Compose acoplado localmente via Room SQLite.",
                                            style = TextStyle(fontSize = 12.sp, color = Color.DarkGray),
                                            lineHeight = 17.sp
                                        )
                                        Text(
                                            text = "Tem alguma sugestão? Escreva abaixo o seu feedback e nós analisaremos:",
                                            style = TextStyle(fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                                        )
                                        var feedbackText by remember { mutableStateOf("") }
                                        OutlinedTextField(
                                            value = feedbackText,
                                            onValueChange = { feedbackText = it },
                                            placeholder = { Text("Insira sua sugestão...") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF1A73E8),
                                                unfocusedBorderColor = Color(0xFFCCCCCC)
                                            ),
                                            textStyle = TextStyle(color = Color.Black, fontSize = 13.5.sp),
                                            modifier = Modifier.fillMaxWidth().height(100.dp)
                                        )
                                        Button(
                                            onClick = {
                                                feedbackText = ""
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Enviar Feedback", color = Color.White)
                                        }
                                    }
                                }

                                "help" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Ajuda e Walkthrough",
                                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                                        )
                                        Text(
                                            text = "1. Como escolher a voz ativa?\nAcesse Inteligência Pessoal para selecionar os tons (Modo História, Agente, Professor etc).\n\n2. Carregar Imagens ou Multimídia?\nToque no botão '+' e escolha arquivos de imagens no dispositivo para perguntar sobre eles.\n\n3. Sincronizar conversas?\nHabilite a toggle 'Importar memória' para que a inteligência faça uma colheita prévia de tudo o que foi conversado.",
                                            style = TextStyle(fontSize = 12.sp, color = Color.DarkGray),
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom dismiss bar
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    iconTint: Color = Color(0xFF444746),
    titleColor: Color = Color(0xFF1F1F1F),
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = titleColor
                )
            )
        }
        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF757575),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4285F4)
) {
    CircularProgressIndicator(
        color = color,
        strokeWidth = 3.dp,
        modifier = modifier.size(24.dp)
    )
}

@Composable
fun UpgradeAdvancedDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F20)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4285F4).copy(alpha = 0.25f),
                                    Color(0xFF9B72F4).copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF8AB4F8),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Upgrade para Gemini Advanced",
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Aproveite recursos exclusivos de última geração: tempo de resposta otimizado sob forte tráfego mundial, modelos de ponta baseados no motor Ultra e capacidades de processamento avançadas para imagens e arquivos de dados densos.",
                    style = TextStyle(
                        fontSize = 12.5.sp,
                        color = Color.LightGray,
                        lineHeight = 17.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = Color.White.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Benefícios Premium inclusos:",
                            style = TextStyle(fontSize = 11.5.sp, color = Color(0xFF8AB4F8), fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "• Modelos ultra velozes de última geração\n• Cota de limite de tokens multiplicada\n• Análise automatizada de planilhas e diagramas complexos",
                            style = TextStyle(fontSize = 11.sp, color = Color.LightGray, lineHeight = 16.sp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Depois", color = Color.Gray)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Iniciar Teste", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun RenameSessionDialog(
    session: ChatSession,
    currentText: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F20)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Editar título do Chat",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )

                OutlinedTextField(
                    value = currentText,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    placeholder = { Text("Insira o novo título...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4285F4),
                        unfocusedBorderColor = Color(0xFF444746)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Salvar", color = Color.White)
                    }
                }
            }
        }
    }
}

// Gorgeous Canvas live editor modal dialog
@Composable
fun CanvasDialog(
    code: String,
    onCodeChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1F22),
            border = BorderStroke(1.dp, Color.DarkGray),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Canvas Nakamura",
                        color = Color(0xFFFF8AA4),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.LightGray)
                    }
                }

                // Scrollable script text field container
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Color.DarkGray),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = code,
                        onValueChange = onCodeChange,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFA9B7C6)
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    
                    TextButton(onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
                        android.widget.Toast.makeText(context, "Código copiado!", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Copiar", color = Color(0xFF8AB4F8))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                android.widget.Toast.makeText(context, "Código executado com sucesso!", android.widget.Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004A77))
                        ) {
                            Text("Executar", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// Slid-up custom sheet mimicking bottom drawer from photo

// True superellipse squircle shape implementation matching premium design guidelines
val SquircleShape: androidx.compose.ui.graphics.Shape = object : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path().apply {
            val w = size.width
            val h = size.height
            // High fidelity cubic Bezier squircle shape
            moveTo(0f, h / 2f)
            cubicTo(0f, h * 0.08f, w * 0.08f, 0f, w / 2f, 0f)
            cubicTo(w * 0.92f, 0f, w, h * 0.08f, w, h / 2f)
            cubicTo(w, h * 0.92f, w * 0.92f, h, w / 2f, h)
            cubicTo(w * 0.08f, h, 0f, h * 0.92f, 0f, h / 2f)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

@Composable
fun GeminiNavigationDrawer(
    sessions: List<ChatSession>,
    currentSessionId: Long?,
    isLoggedIn: Boolean,
    currentUserDisplayName: String?,
    isGenerating: Boolean,
    onConfiguracoesClick: () -> Unit,
    onNovaConversaClick: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onSessionDelete: (Long) -> Unit,
    onSessionShare: () -> Unit,
    onSessionPin: () -> Unit,
    onSessionRename: (ChatSession) -> Unit,
    onTriggerUpgrade: () -> Unit,
    onCloseDrawer: () -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var recentsExpanded by remember { mutableStateOf(true) }
    var menuSessionId by remember { mutableStateOf<Long?>(null) }
    
    val filteredSessions = remember(sessions, searchQuery) {
        if (searchQuery.isBlank()) {
            sessions
        } else {
            sessions.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    val geminiSparkleColors = listOf(
        Color(0xFF4285F4),
        Color(0xFF9B72F4),
        Color(0xFFF072B6)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131314)) // Soft premium dark background
            .padding(vertical = 16.dp, horizontal = 12.dp)
            .navigationBarsPadding() // Keep it safe from system gestures/navigation bar
    ) {
        // 1. Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SparkleIcon(
                    size = 24,
                    colors = geminiSparkleColors,
                    isThinking = isGenerating
                )
                Text(
                    text = "Nakamura IA",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                )
            }
            IconButton(
                onClick = { onCloseDrawer() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MenuOpen,
                    contentDescription = "Collapse menu",
                    tint = Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Add New chat Squircle Pill Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Surface(
                onClick = onNovaConversaClick,
                shape = SquircleShape,
                color = Color(0xFF2E3032),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New chat",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Nova conversa",
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Search chats segment
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF202124))
                .border(1.dp, Color(0xFF3C4043), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text("Buscar conversas...", color = Color.Gray, fontSize = 14.sp)
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. "Recentes" Expandable Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { recentsExpanded = !recentsExpanded }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Recentes",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.LightGray
                )
            )
            Icon(
                imageVector = if (recentsExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = "Toggle recents",
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(
            visible = recentsExpanded,
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (filteredSessions.isEmpty()) {
                    item {
                        Text(
                            text = if (searchQuery.isBlank()) "Sem conversas recentes" else "Nenhuma correspondência",
                            style = TextStyle(fontSize = 13.sp, color = Color.Gray),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                        )
                    }
                } else {
                    items(filteredSessions, key = { it.id }) { session ->
                        val isSelected = session.id == currentSessionId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(0xFF2D2E30) else Color.Transparent
                                )
                                .clickable {
                                    onSessionClick(session.id)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = session.title,
                                    style = TextStyle(
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color.LightGray
                                    ),
                                    maxLines = 1,
                                    modifier = Modifier.testTag("session_item_title_${session.id}")
                                )
                            }
                            
                            Box {
                                IconButton(
                                    onClick = { menuSessionId = session.id },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Session options",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuSessionId == session.id,
                                    onDismissRequest = { menuSessionId = null },
                                    modifier = Modifier.background(Color(0xFF202124))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Compartilhar", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Share, "Share", tint = Color.White, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            menuSessionId = null
                                            onSessionShare()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Fixar", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.PushPin, "Pin", tint = Color.White, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            menuSessionId = null
                                            onSessionPin()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Renomear", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Edit, "Rename", tint = Color.White, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            menuSessionId = null
                                            onSessionRename(session)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Excluir", color = Color.Red) },
                                        leadingIcon = { Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            menuSessionId = null
                                            onSessionDelete(session.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Upgrade Premium Button (Squircle shaped!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Surface(
                onClick = onTriggerUpgrade,
                shape = SquircleShape,
                color = Color(0xFFC5E1A5), // Soft pastel green for premium feels
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF1B5E20),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upgrade Nakamura Pro",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 6. Profile & Settings bottom segment
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(SquircleShape)
                        .background(if (isLoggedIn) Color(0xFF4285F4) else Color(0xFF5F6368)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = if (isLoggedIn) {
                        val parts = (currentUserDisplayName ?: "Usuário").split(" ")
                        if (parts.size >= 2) "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                        else parts[0].take(2).uppercase()
                    } else {
                        "CV"
                    }
                    Text(
                        text = initials,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    )
                }
                Column {
                    Text(
                        text = if (isLoggedIn) (currentUserDisplayName ?: "Usuário") else "Convidado",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = if (isLoggedIn) "Autenticado" else "Modo de Teste",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    )
                }
            }

            IconButton(
                onClick = onConfiguracoesClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configurações",
                    tint = Color.LightGray,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NakamuraPlusSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    activeModeExtension: String?,
    onModeSelect: (String?) -> Unit,
    showCanvasDialog: () -> Unit,
    showSettingsDialog: () -> Unit,
    galleryLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    docLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    geminiCardBackground: Color
) {
    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF131314), // Dark theme match
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // Keep safe from Android system navigation bar overlap
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 32.dp), // Extra generous padding for overlap handling
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Scrollable Row of media capture aids
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    QuickActionPill(
                        title = "Câmera",
                        icon = Icons.Outlined.PhotoCamera,
                        onClick = {
                            onDismiss()
                            Toast.makeText(context, "Câmera desativada no emulador", Toast.LENGTH_SHORT).show()
                        }
                    )
                    QuickActionPill(
                        title = "Galeria",
                        icon = Icons.Outlined.Image,
                        onClick = {
                            onDismiss()
                            galleryLauncher.launch("image/*")
                        }
                    )
                    QuickActionPill(
                        title = "Documento",
                        icon = Icons.Outlined.InsertDriveFile,
                        onClick = {
                            onDismiss()
                            docLauncher.launch("*/*")
                        }
                    )
                    QuickActionPill(
                        title = "Localização",
                        icon = Icons.Outlined.LocationOn,
                        onClick = {
                            onDismiss()
                            Toast.makeText(context, "Simulando localização...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.4f), thickness = 1.dp)

                // Nakamura features Grid List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlusSheetListItem(
                        title = "Canvas Nakamura",
                        subtitle = "Abra uma tela com playground de código em tempo real",
                        icon = Icons.Outlined.Dashboard,
                        onClick = {
                            onDismiss()
                            onModeSelect("canvas")
                            showCanvasDialog()
                        }
                    )

                    PlusSheetListItem(
                        title = "Gerar Imagem",
                        subtitle = "Mude o Nakamura IA para modo fotos da web via Pollinations AI",
                        icon = Icons.Outlined.Palette,
                        onClick = {
                            onDismiss()
                            onModeSelect("image")
                        }
                    )

                    // Wikipedia activator Row with custom Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val nextMode = if (activeModeExtension == "wikipedia") null else "wikipedia"
                                onModeSelect(nextMode)
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(SquircleShape)
                                .background(Color(0xFF8AB4F8).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Book,
                                contentDescription = null,
                                tint = Color(0xFF8AB4F8)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ativar Wikipédia",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Resumir fatos da Wikipédia diretamente no chat",
                                color = Color.Gray,
                                fontSize = 11.5.sp
                            )
                        }
                        Switch(
                            checked = activeModeExtension == "wikipedia",
                            onCheckedChange = { checked ->
                                onModeSelect(if (checked) "wikipedia" else null)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFF8AA4),
                                checkedTrackColor = Color(0xFFFF8AA4).copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }

                    PlusSheetListItem(
                        title = "Personalidade",
                        subtitle = "Gerenciar a persona atual Nakamura Inteligência Artificial",
                        icon = Icons.Outlined.Face,
                        onClick = {
                            onDismiss()
                            showSettingsDialog()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// Small visuals cells using custom superellipse squircle shape
@Composable
fun QuickActionPill(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(SquircleShape)
                .background(Color(0xFF242526))
                .border(1.dp, Color.DarkGray.copy(alpha = 0.4f), SquircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = title,
            color = Color.LightGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Descriptive list item using dynamic squircle design
@Composable
fun PlusSheetListItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(SquircleShape)
                .background(Color(0xFFFF8AA4).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFF8AA4)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 11.5.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ArrowForwardIos,
            contentDescription = null,
            tint = Color.DarkGray,
            modifier = Modifier.size(12.dp)
        )
    }
}

// Helper to isolate code blocks in responses
fun parseMsgWithCode(text: String): Triple<String, String?, String> {
    val startIdx = text.indexOf("```")
    if (startIdx != -1) {
        val endIdx = text.indexOf("```", startIdx + 3)
        if (endIdx != -1) {
            val beforeCode = text.substring(0, startIdx).trim()
            val afterCode = text.substring(endIdx + 3).trim()
            val codeWithLang = text.substring(startIdx + 3, endIdx).trim()
            
            var lang = "kotlin"
            var code = codeWithLang
            val firstLineBreak = codeWithLang.indexOf('\n')
            if (firstLineBreak != -1) {
                val potentialLang = codeWithLang.substring(0, firstLineBreak).trim()
                if (potentialLang.isNotEmpty() && potentialLang.all { it.isLetterOrDigit() }) {
                    lang = potentialLang
                    code = codeWithLang.substring(firstLineBreak + 1)
                }
            }
            
            val message = if (beforeCode.isNotEmpty() && afterCode.isNotEmpty()) {
                "$beforeCode\n\n$afterCode"
            } else if (beforeCode.isNotEmpty()) {
                beforeCode
            } else {
                afterCode
            }
            return Triple(message, code, lang)
        }
    }
    return Triple(text, null, "kotlin")
}

@Composable
fun AiMessageCard(
    message: String,
    code: String? = null,
    language: String = "kotlin",
    onEditCode: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1F20), // Dark surface container
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )

                if (code != null) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF111111),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFF2A2A2A)
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1A1A1A))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = language.uppercase(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // Edit button
                                IconButton(
                                    onClick = {
                                        onEditCode(code)
                                        Toast.makeText(context, "Código importado para edição!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar código",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Download/Save button
                                IconButton(
                                    onClick = {
                                        try {
                                            val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                                            val file = java.io.File(dir, "nakamura_code_${System.currentTimeMillis()}.${if(language.lowercase() == "kotlin") "kt" else "txt"}")
                                            file.writeText(code)
                                            Toast.makeText(context, "Código salvo em Downloads!", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Erro ao salvar código!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Baixar código",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Copy button
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
                                        Toast.makeText(context, "Código copiado!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar código",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(
                                    text = code,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    color = Color(0xFFDDDDDD),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAuthenticationScreen(
    viewModel: ChatViewModel
) {
    val context = LocalContext.current
    var isSimulatingAuth by remember { mutableStateOf(false) }
    var showAccountChooser by remember { mutableStateOf(false) }
    var showCustomNameInput by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customEmail by remember { mutableStateOf("") }

    val geminiDarkBackground = Color(0xFF131314)
    val geminiSparkleColors = listOf(
        Color(0xFF4285F4),
        Color(0xFF9B72F4),
        Color(0xFFF072B6)
    )

    // Infinite rotating transition to let the core glow beautifully
    val infiniteTransition = rememberInfiniteTransition(label = "core_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "core_angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(geminiDarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Nakamura AI Rotating Interactive Core Header
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .drawWithContent {
                        drawContent()
                        val stroke = 3.dp.toPx()
                        val rRadius = size.minDimension / 2f
                        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                        val angleOffset = rotationAngle

                        // Outer glowing cosmic ring
                        drawArc(
                            brush = androidx.compose.ui.graphics.Brush.sweepGradient(geminiSparkleColors),
                            startAngle = angleOffset,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = center - androidx.compose.ui.geometry.Offset(rRadius, rRadius),
                            size = androidx.compose.ui.geometry.Size(rRadius * 2, rRadius * 2),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Core Logo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Nakamura IA",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Seu assistente virtual de nova geração com pesquisa contextualizada, geração de imagens e programador inteligente.",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Gray,
                    height = 20.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (isSimulatingAuth) {
                CircularProgressIndicator(
                    color = Color(0xFF4285F4),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Acessando serviços Google Firebase...",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            } else {
                // Official style Google Sign In Button
                Button(
                    onClick = {
                        showAccountChooser = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(50),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(52.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Custom colorful Google G icon rendering
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .drawBehind {
                                    // 4 segments of Google G color style
                                    drawCircle(color = Color(0xFF4285F4), radius = 9.dp.toPx())
                                    drawCircle(color = Color.White, radius = 5.6.dp.toPx())
                                    drawRect(
                                        color = Color.White,
                                        topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        size = androidx.compose.ui.geometry.Size(9.dp.toPx(), 9.dp.toPx())
                                    )
                                    // Clean dynamic simulation for visual perfection
                                }
                        ) {
                            Text(
                                "G",
                                color = Color(0xFF4285F4),
                                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Black),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = "Fazer login com Google",
                            color = Color(0xFF1F1F1F),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Guest option Button
                OutlinedButton(
                    onClick = {
                        viewModel.continueAsGuest()
                        Toast.makeText(context, "Acessando como Visitante (Imagens desabilitadas)", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(52.dp)
                ) {
                    Text(
                        text = "Continuar como Convidado",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Custom accounts sheet simulator dialog
    if (showAccountChooser) {
        ModalBottomSheet(
            onDismissRequest = { showAccountChooser = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Escolha uma conta para prosseguir no app",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F)
                    )
                )

                // Option 1: Murilo Silva
                Surface(
                    onClick = {
                        showAccountChooser = false
                        isSimulatingAuth = true
                        viewModel.loginWithGoogle("Murilo Silva", "murilosilvadac8@gmail.com", null)
                        Toast.makeText(context, "Bem-vindo de volta, Murilo!", Toast.LENGTH_SHORT).show()
                        isSimulatingAuth = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F3F4),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4285F4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("MS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column {
                            Text("Murilo Silva", color = Color(0xFF1F1F1F), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("murilosilvadac8@gmail.com", color = Color.Gray, fontSize = 11.5.sp)
                        }
                    }
                }

                // Option 2: Add custom account
                Surface(
                    onClick = {
                        showAccountChooser = false
                        showCustomNameInput = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F3F4),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adicionar conta",
                            tint = Color.DarkGray
                        )
                        Text("Usar outra conta do Google", color = Color(0xFF1F1F1F), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Modal dialog for entering custom user name
    if (showCustomNameInput) {
        AlertDialog(
            onDismissRequest = { showCustomNameInput = false },
            containerColor = Color.White,
            title = {
                Text("Crie sua conta Google", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Digite seu nome e email para simular a autenticação Google Firebase com sucesso.", fontSize = 12.5.sp, color = Color.DarkGray)
                    
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Nome Completo") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customEmail,
                        onValueChange = { customEmail = it },
                        label = { Text("E-mail Google") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customName.isNotBlank() && customEmail.isNotBlank()) {
                            showCustomNameInput = false
                            isSimulatingAuth = true
                            viewModel.loginWithGoogle(customName, customEmail, null)
                            Toast.makeText(context, "Logado com sucesso!", Toast.LENGTH_SHORT).show()
                            isSimulatingAuth = false
                        } else {
                            Toast.makeText(context, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                ) {
                    Text("Conectar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomNameInput = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

