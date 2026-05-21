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
import androidx.compose.ui.graphics.*
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

    // Handle system back navigation to close open drawer safely
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFF0F4F9),
                drawerTonalElevation = 0.dp,
                modifier = Modifier.width(300.dp),
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                ) {
                    // 1. Top Core Header Block matching picture
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SparkleIcon(
                                size = 26,
                                colors = geminiSparkleColors
                            )
                            Text(
                                text = "Nakamura IA",
                                style = TextStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF1F1F1F),
                                    letterSpacing = (-0.5).sp
                                )
                            )
                        }
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.close() } },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MenuOpen,
                                contentDescription = "Collapse menu",
                                tint = Color(0xFF444746),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // 2. Add New chat Rounded Pill Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Surface(
                            onClick = {
                                viewModel.startNewChat()
                                coroutineScope.launch { drawerState.close() }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE3E3E3),
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
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "New chat",
                                    tint = Color(0xFF1F1F1F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "New chat",
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1F1F1F)
                                    )
                                )
                            }
                        }
                    }

                    // 3. Search chats segment header with collapse/expand toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isDrawerSearchExpanded = !isDrawerSearchExpanded }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "BUSCAR HISTÓRICO",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF444746),
                                letterSpacing = 0.8.sp
                            )
                        )
                        Icon(
                            imageVector = if (isDrawerSearchExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle search",
                            tint = Color(0xFF444746),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isDrawerSearchExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF444746),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                textStyle = TextStyle(color = Color(0xFF1F1F1F), fontSize = 14.sp),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text("Buscar conversas...", color = Color(0xFF757575), fontSize = 14.sp)
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 4. Library segment
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                // Informative feedback
                                coroutineScope.launch {
                                    drawerState.close()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Widgets,
                            contentDescription = "Library",
                            tint = Color(0xFF444746),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Library",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF1F1F1F)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFFDDE3EA), modifier = Modifier.padding(vertical = 4.dp))

                    // 5. Expandable "Notebooks" section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { notebooksExpanded = !notebooksExpanded }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Notebooks",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF444746)
                            )
                        )
                        Icon(
                            imageVector = if (notebooksExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = "Toggle notebooks",
                            tint = Color(0xFF444746),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = notebooksExpanded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    // Simulated event to matches design
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New notebook",
                                tint = Color(0xFF444746),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "New notebook",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color = Color(0xFF1F1F1F),
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }
                    }

                    // 6. Expandable "Recents" section holding standard lists
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { recentsExpanded = !recentsExpanded }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Recents",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF444746)
                            )
                        )
                        Icon(
                            imageVector = if (recentsExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = "Toggle recents",
                            tint = Color(0xFF444746),
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
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (sessions.isEmpty()) {
                                item {
                                    Text(
                                        text = "Nenhum histórico recente",
                                        style = TextStyle(fontSize = 13.sp, color = Color.Gray),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                                    )
                                }
                            } else {
                                items(sessions, key = { it.id }) { session ->
                                    val isSelected = session.id == currentSessionId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) Color(0xFFE1E3E5) else Color.Transparent
                                            )
                                            .clickable {
                                                viewModel.selectSession(session.id)
                                                coroutineScope.launch { drawerState.close() }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = session.title,
                                            style = TextStyle(
                                                fontSize = 13.5.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = Color(0xFF1F1F1F)
                                            ),
                                            maxLines = 1,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("session_item_title_${session.id}")
                                        )

                                        Box {
                                            IconButton(
                                                onClick = { menuSessionId = session.id },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "Session options",
                                                    tint = Color(0xFF444746),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = menuSessionId == session.id,
                                                onDismissRequest = { menuSessionId = null }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Renomear") },
                                                    leadingIcon = { Icon(Icons.Default.Edit, "Rename", modifier = Modifier.size(16.dp)) },
                                                    onClick = {
                                                        menuSessionId = null
                                                        sessionToRename = session
                                                        renameText = session.title
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Excluir", color = Color.Red) },
                                                    leadingIcon = { Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(16.dp)) },
                                                    onClick = {
                                                        menuSessionId = null
                                                        viewModel.deleteSession(session.id)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 7. Bottom segment: Upgrade button styled like the picture
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Surface(
                            onClick = { showUpgradeDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFD3E3FD),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF041E49),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Upgrade",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF041E49)
                                    )
                                )
                            }
                        }
                    }

                    // 8. Custom Profile section at the bottom representing Murilo Silva
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8AB4F8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "MS",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF041E49),
                                        fontSize = 13.sp
                                    )
                                )
                            }
                            Text(
                                text = "Murilo Silva",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1F1F1F)
                                )
                            )
                        }

                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFF444746),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
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
                                val readableModelName = "Nano GPT Nakamura 2.5"
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
                                    text = { Text("Nano GPT Nakamura 2.5", color = Color.White) },
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
                        IconButton(onClick = { isTopSearchVisible = !isTopSearchVisible }) {
                            Icon(
                                imageVector = if (isTopSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Toggle search bar",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { viewModel.startNewChat() }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Start manual chat",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4A4458))
                                .border(1.dp, Color(0xFF938F99), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "JD",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEADDFF),
                                    fontSize = 13.sp
                                )
                            )
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
                        WelcomeSplashScreen(
                            colors = geminiSparkleColors,
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
                                MessageBubble(
                                    message = msg,
                                    colors = geminiSparkleColors,
                                    cardColor = geminiCardBackground,
                                    accentColor = geminiAccentBlue
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
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
                            // Plus sign multimodal icon
                            IconButton(
                                onClick = { showPlusSheet = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add image attachment",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

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
                                            viewModel.sendMessage()
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
                                            viewModel.sendMessage()
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
}

// Nakamura IA custom logo icon from Cloudinary loaded via Coil AsyncImage
@Composable
fun SparkleIcon(
    size: Int = 40,
    colors: List<Color> = emptyList(),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        coil.compose.AsyncImage(
            model = "https://res.cloudinary.com/di9jolpim/image/upload/v1779405775/9_Sem_T%C3%ADtulo_20260521202134_kko5m8.png",
            contentDescription = "Nakamura IA Icon",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
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
    onChipClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "Sparkle scale")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Sparkle scale animation"
            )

            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Large radial glowing background sphere simulation matching the logo theme
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1A73E8).copy(alpha = 0.25f),
                                    Color(0xFF8AB4F8).copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                SparkleIcon(
                    size = 54,
                    colors = colors,
                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                )
            }

            Text(
                text = buildAnnotatedString {
                    append("Hello, ")
                    withStyle(
                        style = SpanStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF4285F4),
                                    Color(0xFF91B9FF),
                                    Color(0xFFD2E3FC)
                                )
                            ),
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append("Murilo")
                    }
                },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "How can I help you today?",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFFC4C7C5),
                    fontWeight = FontWeight.Normal
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Suggestion Chips Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val suggestionItems = listOf(
                    SuggestionChipItem(
                        text = "Help me write a professional email",
                        icon = Icons.Outlined.Lightbulb,
                        iconColor = Color(0xFF8AB4F8)
                    ),
                    SuggestionChipItem(
                        text = "Summarize my recent notes",
                        icon = Icons.Outlined.Edit,
                        iconColor = Color(0xFF8AB4F8)
                    ),
                    SuggestionChipItem(
                        text = "Plan a weekend hiking trip",
                        icon = Icons.Outlined.Explore,
                        iconColor = Color(0xFF8AB4F8)
                    ),
                    SuggestionChipItem(
                        text = "Debug a Python function",
                        icon = Icons.Default.Code,
                        iconColor = Color(0xFF8AB4F8)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SuggestionChipCard(
                        item = suggestionItems[0],
                        onClick = { onChipClick(suggestionItems[0].text) },
                        modifier = Modifier.weight(1f)
                    )
                    SuggestionChipCard(
                        item = suggestionItems[1],
                        onClick = { onChipClick(suggestionItems[1].text) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SuggestionChipCard(
                        item = suggestionItems[2],
                        onClick = { onChipClick(suggestionItems[2].text) },
                        modifier = Modifier.weight(1f)
                    )
                    SuggestionChipCard(
                        item = suggestionItems[3],
                        onClick = { onChipClick(suggestionItems[3].text) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Custom code-formatting & simple parser inside standard chat bubbles
@Composable
fun MessageBubble(
    message: ChatMessage,
    colors: List<Color>,
    cardColor: Color,
    accentColor: Color
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
                    SparkleIcon(size = 18, colors = colors)
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
                                val bytes = android.util.Base64.decode(message.imageBase64, android.util.Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
                        }
                    }

                    // User text container bubble
                    if (message.content.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = 20.dp,
                                bottomEnd = 4.dp
                            ),
                            color = cardColor,
                            modifier = Modifier.border(1.dp, Color.DarkGray, RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = 20.dp,
                                bottomEnd = 4.dp
                            ))
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
                                text = message.content,
                                color = Color(0xFFFFD2D2),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    // Gemini formatting parser to handle block markdown layout gracefully
                    MarkdownContent(
                        text = message.content,
                        cardColor = cardColor
                    )
                }
            }
        }
    }
}

// Formats basic Markdown structures (like Code Snippet blocks) elegantly
@Composable
fun MarkdownContent(
    text: String,
    cardColor: Color
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
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.5f),
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
                // Plain formatted paragraph
                Text(
                    text = block.content,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp
                    )
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 44.dp, top = 8.dp, bottom = 12.dp)
    ) {
        LoadingIndicator(
            modifier = Modifier.size(20.dp),
            color = colors.firstOrNull() ?: Color(0xFF4285F4)
        )
        Text(
            text = "Nakamura IA está pensando...",
            style = TextStyle(
                fontSize = 13.sp,
                color = Color.LightGray.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        )
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
                tint = Color(0xFF444746),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF1F1F1F)
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
