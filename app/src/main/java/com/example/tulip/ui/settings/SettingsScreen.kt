package com.example.tulip.ui.settings

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tulip.data.ai.AiProviderType
import com.example.tulip.data.ai.OllamaConnectionState
import com.example.tulip.ui.TulipViewModel
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

@Composable
fun SettingsScreen(
    viewModel: TulipViewModel,
    onShowOnboardingAgain: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedProvider by viewModel.selectedAiProvider.collectAsState()
    val ollamaUrl by viewModel.ollamaUrl.collectAsState()
    val ollamaModel by viewModel.ollamaModel.collectAsState()
    val ollamaTestState by viewModel.ollamaTestState.collectAsState()

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showGeminiModelDialog by remember { mutableStateOf(false) }
    var geminiModelInput by remember { mutableStateOf(viewModel.geminiAiService.activeModel) }

    var inputOllamaUrl by remember(ollamaUrl) { mutableStateOf(ollamaUrl) }
    var inputOllamaModel by remember(ollamaModel) { mutableStateOf(ollamaModel) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TulipBackground)
    ) {
        // Header
        Surface(
            color = TulipSurface,
            tonalElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = TulipPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Settings & Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TulipOnSurface
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // About Tulip Card
            Card(
                colors = CardDefaults.cardColors(containerColor = TulipSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = TulipPrimaryTint,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Spa, contentDescription = null, tint = TulipPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Tulip", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TulipOnSurface)
                            Text(text = "Version 1.0 • Field Notebook", fontSize = 12.sp, color = TulipOnSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "A personal AI-powered nature and camping journal designed for quiet field observations, botanical memories, and outdoor exploration without distractions.",
                        fontSize = 13.sp,
                        color = TulipOnSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onShowOnboardingAgain) {
                        Text("Review Introduction Guide", fontSize = 13.sp, color = TulipPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // AI Provider Selection Section
            Card(
                colors = CardDefaults.cardColors(containerColor = TulipSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "AI Provider", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TulipOnSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose which intelligence engine powers 'Fix & Translate to English' and multimodal 'Ask AI' observations.",
                        fontSize = 12.sp,
                        color = TulipOnSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 1: Gemini (Default)
                    Surface(
                        color = if (selectedProvider == AiProviderType.GEMINI) TulipPrimaryTint.copy(alpha = 0.5f) else TulipSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedProvider == AiProviderType.GEMINI) TulipPrimary else TulipOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setAiProvider(AiProviderType.GEMINI) }
                            .testTag("provider_radio_gemini")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedProvider == AiProviderType.GEMINI,
                                onClick = { viewModel.setAiProvider(AiProviderType.GEMINI) },
                                colors = RadioButtonDefaults.colors(selectedColor = TulipPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gemini Flash (Default Cloud)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = TulipOnSurface
                                )
                                Text(
                                    text = "Model: ${viewModel.geminiAiService.activeModel} • Fast, zero local setup",
                                    fontSize = 12.sp,
                                    color = TulipOnSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 2: Local Ollama (Gemma 3 4B)
                    Surface(
                        color = if (selectedProvider == AiProviderType.OLLAMA) TulipPrimaryTint.copy(alpha = 0.5f) else TulipSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedProvider == AiProviderType.OLLAMA) TulipPrimary else TulipOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setAiProvider(AiProviderType.OLLAMA) }
                            .testTag("provider_radio_ollama")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedProvider == AiProviderType.OLLAMA,
                                onClick = { viewModel.setAiProvider(AiProviderType.OLLAMA) },
                                colors = RadioButtonDefaults.colors(selectedColor = TulipPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Local Ollama (Gemma 3 4B)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = TulipOnSurface
                                )
                                Text(
                                    text = "Multimodal inference running on your local Wi-Fi server/laptop",
                                    fontSize = 12.sp,
                                    color = TulipOnSurfaceVariant
                                )
                            }
                        }
                    }

                    // Configuration sub-panel depending on selected provider
                    if (selectedProvider == AiProviderType.GEMINI) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { showGeminiModelDialog = true },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text("Configure Gemini Model Identifier", fontSize = 13.sp, color = TulipPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        // Ollama Configuration Sub-Section
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = TulipOutline.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Ollama Connection Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TulipPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Note: 'localhost' on Android refers to this phone, not your host computer. Enter your laptop's reachable LAN IP (e.g. http://192.168.1.100:11434). Ensure your computer runs Ollama with OLLAMA_HOST=0.0.0.0.",
                            fontSize = 11.sp,
                            color = TulipOnSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Server URL input
                        OutlinedTextField(
                            value = inputOllamaUrl,
                            onValueChange = {
                                inputOllamaUrl = it
                                viewModel.updateOllamaConfig(it, inputOllamaModel)
                                viewModel.resetOllamaTestState()
                            },
                            label = { Text("Ollama Server URL") },
                            placeholder = { Text("http://192.168.1.100:11434") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ollama_url_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TulipPrimary,
                                unfocusedBorderColor = TulipOutline,
                                focusedContainerColor = TulipSurface,
                                unfocusedContainerColor = TulipSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Model identifier input (default: gemma3:4b)
                        OutlinedTextField(
                            value = inputOllamaModel,
                            onValueChange = {
                                inputOllamaModel = it
                                viewModel.updateOllamaConfig(inputOllamaUrl, it)
                                viewModel.resetOllamaTestState()
                            },
                            label = { Text("Ollama Model Identifier") },
                            placeholder = { Text("gemma3:4b") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ollama_model_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TulipPrimary,
                                unfocusedBorderColor = TulipOutline,
                                focusedContainerColor = TulipSurface,
                                unfocusedContainerColor = TulipSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Test Connection Button
                        Button(
                            onClick = {
                                viewModel.updateOllamaConfig(inputOllamaUrl, inputOllamaModel)
                                viewModel.testOllamaConnection()
                            },
                            enabled = ollamaTestState !is OllamaConnectionState.Testing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_ollama_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TulipPrimaryTint,
                                contentColor = TulipPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (ollamaTestState is OllamaConnectionState.Testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = TulipPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connecting to Ollama...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test Connection", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Status Output Box
                        Spacer(modifier = Modifier.height(8.dp))
                        when (val state = ollamaTestState) {
                            is OllamaConnectionState.Idle -> {
                                Text(
                                    text = "Status: Not verified yet. Tap Test Connection to check.",
                                    fontSize = 11.sp,
                                    color = TulipOnSurfaceVariant
                                )
                            }
                            is OllamaConnectionState.Testing -> {
                                Text(
                                    text = "Status: Sending ping & querying /api/tags...",
                                    fontSize = 11.sp,
                                    color = TulipPrimary
                                )
                            }
                            is OllamaConnectionState.Connected -> {
                                Surface(
                                    color = TulipPrimaryTint,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TulipPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Connected Successfully!",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TulipPrimary
                                            )
                                            Text(
                                                text = "Model '${state.modelName}' is installed and ready for multimodal inference.",
                                                fontSize = 11.sp,
                                                color = TulipOnSurface
                                            )
                                        }
                                    }
                                }
                            }
                            is OllamaConnectionState.ModelMissing -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Model Missing on Ollama Server",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = "Server is reachable, but '${state.requestedModel}' is not pulled. Run on your host machine:\nollama pull ${state.requestedModel}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                            is OllamaConnectionState.Error -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Connection Failed",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = state.message,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Privacy & Disclosure Card
            Card(
                colors = CardDefaults.cardColors(containerColor = TulipSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = TulipPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Privacy & Data Storage", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TulipOnSurface)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Local-First: All notes, dates, and photographs are stored strictly on your device inside app sandbox storage.\n" +
                               "• Explicit AI Transmission: Data only leaves your device when you deliberately tap 'Fix & Translate' or 'Ask AI'.\n" +
                               "• Provider Routing: When Local Ollama is chosen, text and images stay within your private local network without reaching external cloud servers.\n" +
                               "• Location Privacy: Location tagging is manual and optional. Tulip never tracks you in the background.",
                        fontSize = 12.sp,
                        color = TulipOnSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            // Data Management & Export
            Card(
                colors = CardDefaults.cardColors(containerColor = TulipSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Data Management", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TulipOnSurface)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Export entries
                    Surface(
                        color = TulipSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    val jsonExport = viewModel.repository.exportEntriesJson()
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, jsonExport)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Export Tulip Journal Data")
                                    context.startActivity(shareIntent)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = TulipPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Export Field Journal (JSON)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TulipOnSurface)
                                Text(text = "Save or share portable backup of all entries and observations", fontSize = 11.sp, color = TulipOnSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Clear data
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearConfirmDialog = true }
                            .testTag("clear_all_data_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Delete All Local Journal Data", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                                Text(text = "Irreversibly clears all local entries and cached images", fontSize = 11.sp, color = TulipOnSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Gemini Model identifier dialog
        if (showGeminiModelDialog) {
            AlertDialog(
                onDismissRequest = { showGeminiModelDialog = false },
                title = { Text("Gemini Model Identifier", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Current default is gemini-3.5-flash. You can specify another valid model ID if preferred:", fontSize = 12.sp, color = TulipOnSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = geminiModelInput,
                            onValueChange = { geminiModelInput = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (geminiModelInput.isNotBlank()) {
                            viewModel.geminiAiService.activeModel = geminiModelInput.trim()
                        }
                        showGeminiModelDialog = false
                    }) {
                        Text("Save", fontWeight = FontWeight.Bold, color = TulipPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGeminiModelDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = TulipSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Clear all confirmation dialog
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text("Delete All Journal Data?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("This will permanently delete all local field journal entries and notes on this device. External AI providers may have retained prior requests under their terms of service.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearAllData {
                            showClearConfirmDialog = false
                        }
                    }) {
                        Text("Delete All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = TulipSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
