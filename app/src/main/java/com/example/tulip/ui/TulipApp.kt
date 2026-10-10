package com.example.tulip.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import com.example.tulip.data.model.JournalEntry
import com.example.tulip.ui.journal.EntryEditorScreen
import com.example.tulip.ui.journal.JournalTimelineScreen
import com.example.tulip.ui.journal.PhotoDetailViewer
import com.example.tulip.ui.settings.SettingsScreen
import com.example.tulip.ui.explore.ExploreScreen
import com.example.ui.theme.TulipAccentClay
import com.example.ui.theme.TulipBackground
import com.example.ui.theme.TulipOnSurface
import com.example.ui.theme.TulipOnSurfaceVariant
import com.example.ui.theme.TulipOutline
import com.example.ui.theme.TulipPrimary
import com.example.ui.theme.TulipPrimaryTint
import com.example.ui.theme.TulipSurface
import com.example.ui.theme.TulipSurfaceVariant
import kotlinx.coroutines.launch
import java.io.File

enum class TulipScreen {
    TIMELINE,
    EXPLORE,
    SETTINGS,
    EDITOR
}

@Composable
fun TulipApp(
    viewModel: TulipViewModel
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeEntryForEdit by viewModel.activeEntryForEdit.collectAsState()
    val askAiState by viewModel.askAiState.collectAsState()
    val fullPhotoViewerUri by viewModel.fullPhotoViewerUri.collectAsState()
    val showOnboarding by viewModel.showOnboarding.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Observe snackbar messages from ViewModel
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Top-level back handler for sub-screens
    BackHandler(enabled = currentScreen != TulipScreen.TIMELINE || fullPhotoViewerUri != null || askAiState != null) {
        if (askAiState != null) {
            viewModel.dismissAskAi()
        } else if (fullPhotoViewerUri != null) {
            viewModel.closePhotoViewer()
        } else {
            viewModel.navigateTo(TulipScreen.TIMELINE)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TulipBackground)
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = TulipBackground,
            bottomBar = {
                // Bottom navigation only visible on main tabs (not during focused full-screen editing)
                if (currentScreen != TulipScreen.EDITOR) {
                    TulipBottomBar(
                        currentScreen = currentScreen,
                        onTabSelected = { viewModel.navigateTo(it) }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    TulipScreen.TIMELINE -> {
                        JournalTimelineScreen(
                            viewModel = viewModel,
                            onNewEntryClick = {
                                viewModel.startNewEntry()
                            },
                            onEntryClick = { entry ->
                                viewModel.editEntry(entry)
                            },
                            onPhotoClick = { uri ->
                                viewModel.openPhotoViewer(uri)
                            },
                            onAskAiForPhoto = { uri ->
                                viewModel.openAskAiForImage(uri)
                            }
                        )
                    }
                    TulipScreen.EXPLORE -> {
                        ExploreScreen(
                            viewModel = viewModel,
                            onEntryClick = { entry ->
                                viewModel.editEntry(entry)
                            },
                            onPhotoClick = { uri ->
                                viewModel.openPhotoViewer(uri)
                            }
                        )
                    }
                    TulipScreen.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onShowOnboardingAgain = {
                                viewModel.reopenOnboarding()
                            }
                        )
                    }
                    TulipScreen.EDITOR -> {
                        EntryEditorScreen(
                            viewModel = viewModel,
                            onBackClick = {
                                viewModel.navigateTo(TulipScreen.TIMELINE)
                            },
                            onAskAiForPhoto = { uri ->
                                viewModel.openAskAiForImage(uri)
                            }
                        )
                    }
                }
            }
        }

        // Full-screen high-res photo viewer overlay
        fullPhotoViewerUri?.let { uri ->
            PhotoDetailViewer(
                photoUri = uri,
                onDismiss = { viewModel.closePhotoViewer() },
                onAskAiClick = {
                    viewModel.openAskAiForImage(uri)
                }
            )
        }

        // Contextual Floating Bottom Sheet / Modal Overlay for Ask AI
        askAiState?.let { state ->
            AskAiOverlay(
                state = state,
                onDismiss = { viewModel.dismissAskAi() },
                onSendQuestion = { q -> viewModel.sendAskAiQuestion(q) },
                onCopyAnswer = { answer ->
                    clipboardManager.setText(AnnotatedString(answer))
                    scope.launch { snackbarHostState.showSnackbar("Copied observation to clipboard") }
                },
                onInsertIntoEntry = { answer ->
                    viewModel.insertAiResponseIntoActiveEntry(answer)
                    scope.launch { snackbarHostState.showSnackbar("Appended observation to entry text") }
                }
            )
        }

        // First-launch or requested Onboarding Dialog
        if (showOnboarding) {
            OnboardingDialog(
                onDismiss = { viewModel.completeOnboarding() }
            )
        }
    }
}

@Composable
fun TulipBottomBar(
    currentScreen: TulipScreen,
    onTabSelected: (TulipScreen) -> Unit
) {
    Surface(
        color = TulipSurface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline.copy(alpha = 0.5f))
    ) {
        NavigationBar(
            containerColor = TulipSurface,
            contentColor = TulipOnSurface,
            tonalElevation = 0.dp,
            modifier = Modifier.height(64.dp)
        ) {
            NavigationBarItem(
                selected = currentScreen == TulipScreen.TIMELINE,
                onClick = { onTabSelected(TulipScreen.TIMELINE) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = "Journal Timeline",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = { Text("Journal", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TulipPrimary,
                    selectedTextColor = TulipPrimary,
                    indicatorColor = TulipPrimaryTint,
                    unselectedIconColor = TulipOnSurfaceVariant,
                    unselectedTextColor = TulipOnSurfaceVariant
                ),
                modifier = Modifier.testTag("tab_journal")
            )

            NavigationBarItem(
                selected = currentScreen == TulipScreen.EXPLORE,
                onClick = { onTabSelected(TulipScreen.EXPLORE) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Explore",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = { Text("Explore", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TulipPrimary,
                    selectedTextColor = TulipPrimary,
                    indicatorColor = TulipPrimaryTint,
                    unselectedIconColor = TulipOnSurfaceVariant,
                    unselectedTextColor = TulipOnSurfaceVariant
                ),
                modifier = Modifier.testTag("tab_explore")
            )

            NavigationBarItem(
                selected = currentScreen == TulipScreen.SETTINGS,
                onClick = { onTabSelected(TulipScreen.SETTINGS) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = { Text("Settings", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TulipPrimary,
                    selectedTextColor = TulipPrimary,
                    indicatorColor = TulipPrimaryTint,
                    unselectedIconColor = TulipOnSurfaceVariant,
                    unselectedTextColor = TulipOnSurfaceVariant
                ),
                modifier = Modifier.testTag("tab_settings")
            )
        }
    }
}

/**
 * Ask AI Contextual Bottom Sheet Overlay
 * Preserves the photograph's visual context without navigating away from the journal entry.
 */
@Composable
fun AskAiOverlay(
    state: AskAiUiState,
    onDismiss: () -> Unit,
    onSendQuestion: (String) -> Unit,
    onCopyAnswer: (String) -> Unit,
    onInsertIntoEntry: (String) -> Unit
) {
    var questionText by remember { mutableStateOf("") }
    var showSafetyDisclose by remember { mutableStateOf(!state.privacyDisclosed) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {} // Consume clicks inside sheet
                .imePadding()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = TulipSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TulipPrimaryTint,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Ask AI",
                                    tint = TulipPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ask AI About Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TulipOnSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Ask AI",
                            tint = TulipOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Image thumbnail & question context
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TulipSurfaceVariant)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val file = File(state.imageUri)
                    AsyncImage(
                        model = if (file.exists()) file else state.imageUri,
                        contentDescription = "Attached photo preview",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Field Observation",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TulipPrimary
                        )
                        Text(
                            text = "Ask about species, botanical features, terrain, or details to photograph next.",
                            fontSize = 12.sp,
                            color = TulipOnSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Explicit disclosure before cloud transmission
                if (showSafetyDisclose) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = TulipPrimaryTint.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Privacy disclosure",
                                tint = TulipPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notice: Asking a question sends this photograph and your query to Gemini AI over the internet. Never consume or touch unidentified wild flora or fungi.",
                                fontSize = 11.sp,
                                color = TulipOnSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Preset suggested questions
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Suggestions:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TulipOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestedQuestionChip(
                        text = "Identify species",
                        onClick = { questionText = "What plant or wildlife species is visible in this photograph?" }
                    )
                    SuggestedQuestionChip(
                        text = "Distinguishing features",
                        onClick = { questionText = "What visual features distinguish this from similar species?" }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestedQuestionChip(
                        text = "Next photo tips",
                        onClick = { questionText = "What angle or botanical detail should I photograph next for verification?" }
                    )
                    SuggestedQuestionChip(
                        text = "Describe habitat",
                        onClick = { questionText = "Describe the visible habitat and terrain conditions in this picture." }
                    )
                }

                // User Input
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    placeholder = {
                        Text("e.g. Can you identify this bird? What tree is this?", fontSize = 14.sp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ask_ai_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TulipPrimary,
                        unfocusedBorderColor = TulipOutline,
                        focusedContainerColor = TulipSurface,
                        unfocusedContainerColor = TulipSurface
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Send Button
                Button(
                    onClick = {
                        onSendQuestion(questionText)
                    },
                    enabled = questionText.isNotBlank() && !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ask_ai_send_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TulipPrimary,
                        contentColor = Color.White
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing observation...", fontSize = 14.sp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Question", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Error Display
                state.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = { onSendQuestion(questionText) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // AI Answer Card
                state.responseAnswer?.let { answer ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TulipSurfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AI Observation",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TulipPrimary
                                )
                                Row {
                                    IconButton(
                                        onClick = { onCopyAnswer(answer) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy answer",
                                            tint = TulipOnSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = answer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TulipOnSurface,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = TulipOutline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            // Insert action into journal entry
                            Button(
                                onClick = {
                                    onInsertIntoEntry(answer)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ask_ai_insert_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TulipPrimaryTint,
                                    contentColor = TulipPrimary
                                )
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Insert Observation into Journal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedQuestionChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        color = TulipSurfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = TulipOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TulipSurface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = TulipPrimaryTint,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = "Tulip Logo",
                            tint = TulipPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Welcome to Tulip",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TulipOnSurface
                )
                Text(
                    text = "Your quiet field notebook in the wild",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TulipOnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                OnboardingFeatureItem(
                    title = "Offline Field Journal",
                    desc = "Your thoughts, notes, and photos are saved locally on device. No internet required for writing."
                )
                Spacer(modifier = Modifier.height(14.dp))
                OnboardingFeatureItem(
                    title = "Preserve Original Memories",
                    desc = "Tulip never silently rewrites your words. You choose when to apply Fix & Translate."
                )
                Spacer(modifier = Modifier.height(14.dp))
                OnboardingFeatureItem(
                    title = "Honest AI Nature Identification",
                    desc = "Ask AI examines your actual photographs. It admits uncertainty and never claims wild species are safe to consume."
                )
                Spacer(modifier = Modifier.height(14.dp))
                OnboardingFeatureItem(
                    title = "Privacy & Location Control",
                    desc = "Location is completely optional. Data only leaves your phone when you explicitly trigger an AI feature."
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_continue_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TulipPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Begin Field Journaling", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun OnboardingFeatureItem(title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = TulipPrimary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TulipOnSurface)
            Text(text = desc, fontSize = 12.sp, color = TulipOnSurfaceVariant, lineHeight = 16.sp)
        }
    }
}
