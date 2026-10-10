package com.example.tulip.ui.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExploreScreen(
    viewModel: TulipViewModel,
    onEntryClick: (JournalEntry) -> Unit,
    onPhotoClick: (String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTag by viewModel.selectedTagFilter.collectAsState()
    val filteredEntries by viewModel.filteredEntries.collectAsState()
    val allEntries by viewModel.entries.collectAsState()

    // Collect all unique tags across entries
    val allTags = allEntries.flatMap { entry ->
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(entry.tagsJson)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        } catch (_: Exception) {}
        list
    }.distinct()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TulipBackground)
    ) {
        // Header
        Surface(
            color = TulipSurface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, TulipOutline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Explore, contentDescription = null, tint = TulipPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Explore & Rediscover",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TulipOnSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search memories, species, notes, or places...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TulipOnSurfaceVariant) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TulipOnSurfaceVariant)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TulipPrimary,
                        unfocusedBorderColor = TulipOutline,
                        focusedContainerColor = TulipSurfaceVariant,
                        unfocusedContainerColor = TulipSurfaceVariant
                    ),
                    singleLine = true
                )

                // Tags Filter Row
                if (allTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Surface(
                                color = if (selectedTag == null) TulipPrimary else TulipSurfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.clickable { viewModel.setTagFilter(null) }
                            ) {
                                Text(
                                    text = "All",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedTag == null) androidx.compose.ui.graphics.Color.White else TulipOnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        items(allTags) { tag ->
                            val isSelected = selectedTag == tag
                            Surface(
                                color = if (isSelected) TulipPrimary else TulipSurfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.clickable {
                                    viewModel.setTagFilter(if (isSelected) null else tag)
                                }
                            ) {
                                Text(
                                    text = "#$tag",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) androidx.compose.ui.graphics.Color.White else TulipOnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search and filter results
        if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = TulipOnSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedTag != null) "No matching field notes found" else "No journal entries yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = TulipOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try adjusting your search terms or filter tags.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TulipOnSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredEntries, key = { it.id }) { entry ->
                    ExploreEntryItem(
                        entry = entry,
                        onClick = { onEntryClick(entry) },
                        onPhotoClick = onPhotoClick
                    )
                }
            }
        }
    }
}

@Composable
fun ExploreEntryItem(
    entry: JournalEntry,
    onClick: () -> Unit,
    onPhotoClick: (String) -> Unit
) {
    val firstPhoto: String? = remember(entry.imageUrisJson) {
        try {
            val arr = JSONArray(entry.imageUrisJson)
            if (arr.length() > 0) arr.getString(0) else null
        } catch (_: Exception) {
            null
        }
    }

    val formattedDate = remember(entry.createdAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.format(Date(entry.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = TulipSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TulipOutline.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            firstPhoto?.let { photoUri ->
                val file = File(photoUri)
                AsyncImage(
                    model = if (file.exists()) file else photoUri,
                    contentDescription = "Entry photo thumbnail",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPhotoClick(photoUri) },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TulipPrimary
                    )
                    entry.locationLabel?.let { loc ->
                        Text(
                            text = loc,
                            fontSize = 11.sp,
                            color = TulipAccentClay,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = entry.originalText.ifEmpty { "[Photo observation without written note]" },
                    fontSize = 13.sp,
                    color = TulipOnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                if (!entry.translatedEnglishText.isNullOrBlank() && entry.translatedEnglishText != entry.originalText) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "EN: ${entry.translatedEnglishText}",
                        fontSize = 11.sp,
                        color = TulipOnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
