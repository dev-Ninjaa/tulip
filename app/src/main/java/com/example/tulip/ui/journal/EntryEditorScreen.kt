package com.example.tulip.ui.journal

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.tulip.ui.TulipViewModel
import com.example.ui.theme.TulipAccentClay
import com.example.ui.theme.TulipBackground
import com.example.ui.theme.TulipOnSurface
import com.example.ui.theme.TulipOnSurfaceVariant
import com.example.ui.theme.TulipOutline
import com.example.ui.theme.TulipPrimary
import com.example.ui.theme.TulipPrimaryTint
import com.example.ui.theme.TulipSurface
import com.example.ui.theme.TulipSurfaceSubtle
import com.example.ui.theme.TulipSurfaceVariant
import java.io.File

@Composable
fun EntryEditorScreen(
    viewModel: TulipViewModel,
    onBackClick: () -> Unit,
    onAskAiForPhoto: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val originalText by viewModel.editorOriginalText.collectAsState()
    val images by viewModel.editorImages.collectAsState()
    val locationLabel by viewModel.editorLocationLabel.collectAsState()
    val tags by viewModel.editorTags.collectAsState()
    val fixTranslateState by viewModel.fixTranslateState.collectAsState()
    val activeEntry by viewModel.activeEntryForEdit.collectAsState()

    var showLocationDialog by remember { mutableStateOf(false) }
    var locationInput by remember { mutableStateOf(locationLabel ?: "") }
    var showTagDialog by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Camera photo temporary Uri
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraTempUri != null) {
            viewModel.attachImageUri(cameraTempUri!!)
        }
    }

    // Google Play compliant zero-permission Photo Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachImageUri(uri)
        }
    }

    fun launchCamera() {
        try {
            val cacheDir = File(context.cacheDir, "camera_temp")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val tempFile = File(cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            cameraTempUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            viewModel.attachImageUri(Uri.EMPTY)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TulipBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Top Editor Bar
            Surface(
                color = TulipSurface,
                tonalElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TulipOnSurface)
                    }

                    Text(
                        text = if (activeEntry != null) "Edit Field Note" else "New Field Note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TulipOnSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (activeEntry != null) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Entry", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Button(
                            onClick = { viewModel.saveActiveEntry(isDraft = false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TulipPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_entry_button")
                        ) {
                            Text("Save", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Scrollable writing and photo area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Photo Attachment Row
                Text(
                    text = "Photographs",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TulipOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Camera Action Button
                    item {
                        Surface(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { launchCamera() },
                            color = TulipSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", tint = TulipPrimary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Camera", fontSize = 11.sp, color = TulipPrimary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Gallery Photo Picker Button
                    item {
                        Surface(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            color = TulipSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = TulipPrimary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Gallery", fontSize = 11.sp, color = TulipPrimary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Attached Photo Thumbnails
                    items(images) { photoPath ->
                        val file = File(photoPath)
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, TulipOutline, RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = if (file.exists()) file else photoPath,
                                contentDescription = "Attached photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onAskAiForPhoto(photoPath) },
                                contentScale = ContentScale.Crop
                            )
                            // Remove image button
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .clickable { viewModel.removeImage(photoPath) }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.padding(3.dp))
                            }
                            // Ask AI tag badge
                            Surface(
                                color = TulipSurface.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .clickable { onAskAiForPhoto(photoPath) }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TulipPrimary, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Free-form multiline writing area
                Text(
                    text = "Observations & Memories",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TulipOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = originalText,
                    onValueChange = { viewModel.updateEditorText(it) },
                    placeholder = {
                        Text(
                            "Record what you see, smell, or hear... Write freely in any language or dialect.",
                            fontSize = 15.sp,
                            color = TulipOnSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 22.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("editor_text_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TulipPrimary,
                        unfocusedBorderColor = TulipOutline,
                        focusedContainerColor = TulipSurface,
                        unfocusedContainerColor = TulipSurface
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Real "Fix & Translate to English" Feature Button
                Surface(
                    color = TulipSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Fix & Translate",
                                    tint = TulipPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Fix & Translate to English",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TulipOnSurface
                                )
                            }

                            Button(
                                onClick = { viewModel.requestFixAndTranslate() },
                                enabled = originalText.isNotBlank() && !fixTranslateState.isLoading,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TulipPrimary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("fix_translate_button")
                            ) {
                                if (fixTranslateState.isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Refining...", fontSize = 12.sp)
                                } else {
                                    Text("Refine", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Informational disclosure
                        Text(
                            text = "Preserves your tone and nuance while repairing spelling, grammar, or translating into natural English. Requires internet connection.",
                            fontSize = 11.sp,
                            color = TulipOnSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // Fix & Translate Error
                        fixTranslateState.errorMessage?.let { errorMsg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMsg,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        // Fix & Translate Suggestion Preview
                        fixTranslateState.suggestedEnglishText?.let { suggestion ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TulipSurface),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (fixTranslateState.isUnchangedOrMinimal) "Original Text Verified Accurate" else "Suggested English Version:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TulipPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = suggestion,
                                        fontSize = 13.sp,
                                        color = TulipOnSurface,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(suggestion))
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy", fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { viewModel.applyFixAndTranslate() },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = TulipPrimaryTint,
                                                contentColor = TulipPrimary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("apply_translation_button")
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Apply to Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Location & Tags Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Field Context & Tags",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TulipOnSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Location row
                Surface(
                    color = TulipSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLocationDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = TulipAccentClay, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = locationLabel ?: "Add location (e.g. Pine Ridge Trail, Blackwood Waterfall)",
                            fontSize = 13.sp,
                            color = if (locationLabel != null) TulipOnSurface else TulipOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tags Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tags.forEach { tag ->
                        Surface(
                            color = TulipSurfaceSubtle,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#$tag", fontSize = 12.sp, color = TulipPrimary, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove tag",
                                    tint = TulipOnSurfaceVariant,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.removeTag(tag) }
                                )
                            }
                        }
                    }

                    // Add Tag Button
                    Surface(
                        color = TulipSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline),
                        modifier = Modifier.clickable { showTagDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = TulipPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tag", fontSize = 12.sp, color = TulipPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Draft / Save bottom bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.saveActiveEntry(isDraft = true) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TulipOnSurfaceVariant)
                    ) {
                        Text("Save Draft", fontSize = 13.sp)
                    }

                    Button(
                        onClick = { viewModel.saveActiveEntry(isDraft = false) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TulipPrimary, contentColor = Color.White)
                    ) {
                        Text("Preserve Entry", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Location Edit Dialog
        if (showLocationDialog) {
            AlertDialog(
                onDismissRequest = { showLocationDialog = false },
                title = { Text("Set Field Location", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Add an optional campsite, forest, or trail landmark. Tulip does not track you automatically.", fontSize = 13.sp, color = TulipOnSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = locationInput,
                            onValueChange = { locationInput = it },
                            placeholder = { Text("e.g. High Cascade Ridge") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setLocationLabel(locationInput.trim().ifEmpty { null })
                        showLocationDialog = false
                    }) {
                        Text("Set Location", fontWeight = FontWeight.Bold, color = TulipPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLocationDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = TulipSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Tag Add Dialog
        if (showTagDialog) {
            AlertDialog(
                onDismissRequest = { showTagDialog = false },
                title = { Text("Add Observation Tag", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            placeholder = { Text("e.g. fungi, wildflowers, birds, canopy") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (tagInput.isNotBlank()) {
                            viewModel.addTag(tagInput)
                            tagInput = ""
                        }
                        showTagDialog = false
                    }) {
                        Text("Add", fontWeight = FontWeight.Bold, color = TulipPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTagDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = TulipSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Delete Confirm Dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete This Entry?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this field note? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteActiveEntry()
                        showDeleteConfirm = false
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = TulipSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
