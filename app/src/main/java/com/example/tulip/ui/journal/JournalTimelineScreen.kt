package com.example.tulip.ui.journal

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tulip.data.model.JournalEntry
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
import org.json.JSONArray
import java.io.File
import java.util.Date

@Composable
fun JournalTimelineScreen(
    viewModel: TulipViewModel,
    onNewEntryClick: () -> Unit,
    onEntryClick: (JournalEntry) -> Unit,
    onPhotoClick: (String) -> Unit,
    onAskAiForPhoto: (String) -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    var entryToDelete by remember { mutableStateOf<JournalEntry?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TulipBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Calm, understated Header
            TimelineHeader()

            if (entries.isEmpty()) {
                // Empty state designed specifically for nature journaling
                EmptyJournalTimeline(onNewEntryClick = onNewEntryClick)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("journal_timeline_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        JournalEntryCard(
                            entry = entry,
                            onCardClick = { onEntryClick(entry) },
                            onPhotoClick = onPhotoClick,
                            onAskAiForPhoto = onAskAiForPhoto,
                            onDeleteClick = { entryToDelete = entry }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp)) // Clearance for FAB
                    }
                }
            }
        }

        // Floating Action Button for New Entry
        FloatingActionButton(
            onClick = onNewEntryClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("new_entry_fab"),
            containerColor = TulipPrimary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New Entry", modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Note", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Delete Confirmation Dialog
        entryToDelete?.let { entry ->
            AlertDialog(
                onDismissRequest = { entryToDelete = null },
                title = { Text("Delete Journal Entry?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("This action will remove this entry and its local notes from your journal. It cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteEntry(entry)
                            entryToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { entryToDelete = null }) {
                        Text("Cancel")
                    }
                },
                containerColor = TulipSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun TimelineHeader() {
    Surface(
        color = TulipSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, color = TulipOutline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = TulipPrimaryTint,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = "Tulip Icon",
                            tint = TulipPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Tulip",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TulipOnSurface,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Field Notes & Observations",
                        style = MaterialTheme.typography.bodySmall,
                        color = TulipOnSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalEntryCard(
    entry: JournalEntry,
    onCardClick: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onAskAiForPhoto: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val images = remember(entry.imageUrisJson) {
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(entry.imageUrisJson)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        } catch (_: Exception) {}
        list
    }

    val tags = remember(entry.tagsJson) {
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(entry.tagsJson)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        } catch (_: Exception) {}
        list
    }

    val formattedDate = remember(entry.createdAt) {
        DateFormat.format("EEE, MMM d, yyyy • h:mm a", Date(entry.createdAt)).toString()
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onCardClick)
            .testTag("entry_card_${entry.id}"),
        colors = CardDefaults.cardColors(containerColor = TulipSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline.copy(alpha = 0.7f))
    ) {
        Column {
            // Attached Photographs carousel / single image
            if (images.isNotEmpty()) {
                if (images.size == 1) {
                    val singleUri = images.first()
                    val file = File(singleUri)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(TulipSurfaceVariant)
                            .combinedClickable(
                                onClick = { onPhotoClick(singleUri) },
                                onLongClick = { onAskAiForPhoto(singleUri) }
                            )
                    ) {
                        AsyncImage(
                            model = if (file.exists()) file else singleUri,
                            contentDescription = "Field observation photograph",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Subtle contextual "Ask AI" badge for quick access and accessibility
                        Surface(
                            color = TulipSurface.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .clickable { onAskAiForPhoto(singleUri) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "Ask AI about this photo",
                                    tint = TulipPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ask AI", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TulipPrimary)
                            }
                        }
                    }
                } else {
                    // Multi-image horizontal scroll
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(images) { photoUri ->
                            val file = File(photoUri)
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(TulipSurfaceVariant)
                                    .combinedClickable(
                                        onClick = { onPhotoClick(photoUri) },
                                        onLongClick = { onAskAiForPhoto(photoUri) }
                                    )
                            ) {
                                AsyncImage(
                                    model = if (file.exists()) file else photoUri,
                                    contentDescription = "Observation photograph",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    color = TulipSurface.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .clickable { onAskAiForPhoto(photoUri) }
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "Ask AI",
                                        tint = TulipPrimary,
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Entry Content
            Column(modifier = Modifier.padding(16.dp)) {
                // Metadata Row: Date, Location, Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelMedium,
                            color = TulipOnSurfaceVariant
                        )
                        entry.locationLabel?.let { loc ->
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    tint = TulipAccentClay,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = loc,
                                    fontSize = 11.sp,
                                    color = TulipAccentClay,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Entry options", tint = TulipOnSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(TulipSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Entry") },
                                onClick = {
                                    menuExpanded = false
                                    onCardClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TulipPrimary) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Entry", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }

                // Text Observation
                if (entry.originalText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = entry.originalText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TulipOnSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }

                // AI Translated English badge if available
                if (!entry.translatedEnglishText.isNullOrBlank() && entry.translatedEnglishText != entry.originalText) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = TulipSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TulipPrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("English Translation", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TulipPrimary)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = entry.translatedEnglishText,
                                fontSize = 12.sp,
                                color = TulipOnSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Tags
                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.take(4).forEach { tag ->
                            Surface(
                                color = TulipSurfaceSubtle,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "#$tag",
                                    fontSize = 11.sp,
                                    color = TulipPrimary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyJournalTimeline(
    onNewEntryClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = TulipPrimaryTint,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = "Empty Journal",
                        tint = TulipPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Your Field Notebook is Empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TulipOnSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Begin capturing moments from your hikes, camping trips, forests, and outdoor explorations.",
                style = MaterialTheme.typography.bodyMedium,
                color = TulipOnSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = onNewEntryClick,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    containerColor = TulipPrimaryTint,
                    contentColor = TulipPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Write Your First Note", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
