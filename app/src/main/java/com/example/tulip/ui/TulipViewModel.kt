package com.example.tulip.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tulip.data.ai.AiProviderType
import com.example.tulip.data.ai.AiResult
import com.example.tulip.data.ai.GeminiAiService
import com.example.tulip.data.ai.NatureAiService
import com.example.tulip.data.ai.OllamaConnectionState
import com.example.tulip.data.ai.ollama.OllamaAiService
import com.example.tulip.data.model.JournalEntry
import com.example.tulip.data.repository.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

data class AskAiUiState(
    val imageUri: String,
    val isLoading: Boolean = false,
    val question: String = "",
    val responseAnswer: String? = null,
    val errorMessage: String? = null,
    val privacyDisclosed: Boolean = false
)

data class FixAndTranslateUiState(
    val isLoading: Boolean = false,
    val suggestedEnglishText: String? = null,
    val errorMessage: String? = null,
    val isUnchangedOrMinimal: Boolean = false
)

class TulipViewModel(application: Application) : AndroidViewModel(application) {

    val repository = JournalRepository(application)
    val geminiAiService = GeminiAiService()
    val ollamaAiService = OllamaAiService()

    private val prefs = application.getSharedPreferences("tulip_prefs", Application.MODE_PRIVATE)

    // AI Provider selection state
    private val _selectedAiProvider = MutableStateFlow(
        AiProviderType.valueOf(
            prefs.getString("selected_ai_provider", AiProviderType.GEMINI.name) ?: AiProviderType.GEMINI.name
        )
    )
    val selectedAiProvider: StateFlow<AiProviderType> = _selectedAiProvider.asStateFlow()

    // Ollama settings state
    private val _ollamaUrl = MutableStateFlow(
        prefs.getString("ollama_server_url", "http://192.168.1.100:11434") ?: "http://192.168.1.100:11434"
    )
    val ollamaUrl: StateFlow<String> = _ollamaUrl.asStateFlow()

    private val _ollamaModel = MutableStateFlow(
        prefs.getString("ollama_model_id", "gemma3:4b") ?: "gemma3:4b"
    )
    val ollamaModel: StateFlow<String> = _ollamaModel.asStateFlow()

    private val _ollamaTestState = MutableStateFlow<OllamaConnectionState>(OllamaConnectionState.Idle)
    val ollamaTestState: StateFlow<OllamaConnectionState> = _ollamaTestState.asStateFlow()

    init {
        ollamaAiService.serverUrl = _ollamaUrl.value
        ollamaAiService.modelId = _ollamaModel.value
    }

    /**
     * Returns the active AI service implementation based on the user's Settings choice.
     */
    val currentAiService: NatureAiService
        get() = when (_selectedAiProvider.value) {
            AiProviderType.GEMINI -> geminiAiService
            AiProviderType.OLLAMA -> ollamaAiService
        }

    // Keep backwards-compatible property for existing references
    val aiService: GeminiAiService
        get() = geminiAiService

    // Navigation state
    private val _currentScreen = MutableStateFlow(TulipScreen.TIMELINE)
    val currentScreen: StateFlow<TulipScreen> = _currentScreen.asStateFlow()

    // Search query & Explore state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    // Timeline entries from DB
    val entries: StateFlow<List<JournalEntry>> = repository.entriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered entries for Explore / Search
    val filteredEntries: StateFlow<List<JournalEntry>> = combine(
        entries,
        _searchQuery,
        _selectedTagFilter
    ) { list, query, tag ->
        var res = list
        if (tag != null) {
            res = res.filter { it.tagsJson.contains(tag, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            res = res.filter { entry ->
                entry.originalText.contains(query, ignoreCase = true) ||
                (entry.translatedEnglishText?.contains(query, ignoreCase = true) == true) ||
                (entry.locationLabel?.contains(query, ignoreCase = true) == true) ||
                entry.tagsJson.contains(query, ignoreCase = true)
            }
        }
        res
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active entry being edited
    private val _activeEntryForEdit = MutableStateFlow<JournalEntry?>(null)
    val activeEntryForEdit: StateFlow<JournalEntry?> = _activeEntryForEdit.asStateFlow()

    // Editor fields
    private val _editorOriginalText = MutableStateFlow("")
    val editorOriginalText: StateFlow<String> = _editorOriginalText.asStateFlow()

    private val _editorImages = MutableStateFlow<List<String>>(emptyList())
    val editorImages: StateFlow<List<String>> = _editorImages.asStateFlow()

    private val _editorLocationLabel = MutableStateFlow<String?>(null)
    val editorLocationLabel: StateFlow<String?> = _editorLocationLabel.asStateFlow()

    private val _editorTags = MutableStateFlow<List<String>>(emptyList())
    val editorTags: StateFlow<List<String>> = _editorTags.asStateFlow()

    // Fix & Translate State
    private val _fixTranslateState = MutableStateFlow(FixAndTranslateUiState())
    val fixTranslateState: StateFlow<FixAndTranslateUiState> = _fixTranslateState.asStateFlow()

    // Ask AI Modal State
    private val _askAiState = MutableStateFlow<AskAiUiState?>(null)
    val askAiState: StateFlow<AskAiUiState?> = _askAiState.asStateFlow()

    // Full photo detail viewer
    private val _fullPhotoViewerUri = MutableStateFlow<String?>(null)
    val fullPhotoViewerUri: StateFlow<String?> = _fullPhotoViewerUri.asStateFlow()

    // Onboarding dialog state
    private val _showOnboarding = MutableStateFlow(!prefs.getBoolean("has_seen_onboarding", false))
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    // Global Snackbar notifications
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun setAiProvider(provider: AiProviderType) {
        _selectedAiProvider.value = provider
        prefs.edit().putString("selected_ai_provider", provider.name).apply()
        _snackbarMessage.value = "AI Provider switched to ${provider.displayName}"
    }

    fun updateOllamaConfig(url: String, model: String) {
        val trimmedUrl = url.trim()
        val trimmedModel = model.trim().ifEmpty { "gemma3:4b" }
        _ollamaUrl.value = trimmedUrl
        _ollamaModel.value = trimmedModel
        ollamaAiService.serverUrl = trimmedUrl
        ollamaAiService.modelId = trimmedModel
        prefs.edit()
            .putString("ollama_server_url", trimmedUrl)
            .putString("ollama_model_id", trimmedModel)
            .apply()
    }

    fun testOllamaConnection() {
        _ollamaTestState.value = OllamaConnectionState.Testing
        viewModelScope.launch {
            val state = ollamaAiService.testConnection()
            _ollamaTestState.value = state
        }
    }

    fun resetOllamaTestState() {
        _ollamaTestState.value = OllamaConnectionState.Idle
    }

    fun navigateTo(screen: TulipScreen) {
        _currentScreen.value = screen
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("has_seen_onboarding", true).apply()
        _showOnboarding.value = false
    }

    fun reopenOnboarding() {
        _showOnboarding.value = true
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun setTagFilter(tag: String?) {
        _selectedTagFilter.value = tag
    }

    fun startNewEntry() {
        _activeEntryForEdit.value = null
        _editorOriginalText.value = ""
        _editorImages.value = emptyList()
        _editorLocationLabel.value = null
        _editorTags.value = emptyList()
        _fixTranslateState.value = FixAndTranslateUiState()
        _currentScreen.value = TulipScreen.EDITOR
    }

    fun editEntry(entry: JournalEntry) {
        _activeEntryForEdit.value = entry
        _editorOriginalText.value = entry.originalText
        _editorLocationLabel.value = entry.locationLabel

        val imgList = mutableListOf<String>()
        try {
            val jsonArr = JSONArray(entry.imageUrisJson)
            for (i in 0 until jsonArr.length()) {
                imgList.add(jsonArr.getString(i))
            }
        } catch (_: Exception) {}
        _editorImages.value = imgList

        val tagsList = mutableListOf<String>()
        try {
            val jsonArr = JSONArray(entry.tagsJson)
            for (i in 0 until jsonArr.length()) {
                tagsList.add(jsonArr.getString(i))
            }
        } catch (_: Exception) {}
        _editorTags.value = tagsList

        _fixTranslateState.value = FixAndTranslateUiState(
            suggestedEnglishText = entry.translatedEnglishText
        )
        _currentScreen.value = TulipScreen.EDITOR
    }

    fun updateEditorText(text: String) {
        _editorOriginalText.value = text
    }

    fun setLocationLabel(location: String?) {
        _editorLocationLabel.value = location
    }

    fun addTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isNotBlank() && !_editorTags.value.contains(trimmed)) {
            _editorTags.value = _editorTags.value + trimmed
        }
    }

    fun removeTag(tag: String) {
        _editorTags.value = _editorTags.value.filter { it != tag }
    }

    fun attachImageUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val localPath = repository.persistImageLocally(uri)
                _editorImages.value = _editorImages.value + localPath
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to copy photo: ${e.localizedMessage}"
            }
        }
    }

    fun removeImage(imagePath: String) {
        _editorImages.value = _editorImages.value.filter { it != imagePath }
    }

    fun openPhotoViewer(uri: String) {
        _fullPhotoViewerUri.value = uri
    }

    fun closePhotoViewer() {
        _fullPhotoViewerUri.value = null
    }

    // --- Core Save & Persistence ---

    fun saveActiveEntry(isDraft: Boolean = false) {
        val text = _editorOriginalText.value.trim()
        val images = _editorImages.value
        val location = _editorLocationLabel.value
        val tags = _editorTags.value
        val fixState = _fixTranslateState.value

        if (text.isEmpty() && images.isEmpty() && !isDraft) {
            _snackbarMessage.value = "Please add text or attach a photo before saving."
            return
        }

        val existing = _activeEntryForEdit.value
        val entry = JournalEntry(
            id = existing?.id ?: UUID.randomUUID().toString(),
            originalText = text,
            translatedEnglishText = fixState.suggestedEnglishText ?: existing?.translatedEnglishText,
            translationApplied = existing?.translationApplied ?: false,
            imageUrisJson = JSONArray(images).toString(),
            locationLabel = location,
            latitude = existing?.latitude,
            longitude = existing?.longitude,
            tagsJson = JSONArray(tags).toString(),
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isDraft = isDraft,
            aiNotesJson = existing?.aiNotesJson ?: "[]"
        )

        viewModelScope.launch {
            repository.saveEntry(entry)
            _snackbarMessage.value = if (isDraft) "Draft saved" else "Journal entry preserved"
            if (!isDraft) {
                _currentScreen.value = TulipScreen.TIMELINE
            }
        }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
            _snackbarMessage.value = "Entry deleted"
            if (_activeEntryForEdit.value?.id == entry.id) {
                _currentScreen.value = TulipScreen.TIMELINE
            }
        }
    }

    fun deleteActiveEntry() {
        val active = _activeEntryForEdit.value
        if (active != null) {
            deleteEntry(active)
        } else {
            _currentScreen.value = TulipScreen.TIMELINE
        }
    }

    // --- Fix & Translate to English (Routed through current provider) ---

    fun requestFixAndTranslate() {
        val text = _editorOriginalText.value.trim()
        if (text.isEmpty()) {
            _fixTranslateState.value = FixAndTranslateUiState(
                errorMessage = "Write a thought or note first before fixing or translating."
            )
            return
        }

        _fixTranslateState.value = FixAndTranslateUiState(isLoading = true)

        viewModelScope.launch {
            val provider = currentAiService
            when (val result = provider.fixAndTranslateToEnglish(text)) {
                is AiResult.Success -> {
                    val corrected = result.data.trim()
                    val isMinimal = corrected.equals(text, ignoreCase = true)
                    _fixTranslateState.value = FixAndTranslateUiState(
                        isLoading = false,
                        suggestedEnglishText = corrected,
                        isUnchangedOrMinimal = isMinimal
                    )
                }
                is AiResult.Error -> {
                    _fixTranslateState.value = FixAndTranslateUiState(
                        isLoading = false,
                        errorMessage = "[${provider.providerType.displayName}] ${result.message}"
                    )
                }
            }
        }
    }

    fun applyFixAndTranslate() {
        val suggested = _fixTranslateState.value.suggestedEnglishText
        if (!suggested.isNullOrBlank()) {
            _editorOriginalText.value = suggested
            _snackbarMessage.value = "Applied English version to journal"
        }
    }

    fun clearFixTranslateSuggestion() {
        _fixTranslateState.value = FixAndTranslateUiState()
    }

    // --- Ask AI Multimodal (Routed through current provider) ---

    fun openAskAiForImage(imageUri: String) {
        _askAiState.value = AskAiUiState(
            imageUri = imageUri,
            privacyDisclosed = prefs.getBoolean("ask_ai_disclosed", false)
        )
    }

    fun dismissAskAi() {
        _askAiState.value = null
    }

    fun sendAskAiQuestion(question: String) {
        val current = _askAiState.value ?: return
        if (question.isBlank()) return

        prefs.edit().putBoolean("ask_ai_disclosed", true).apply()

        _askAiState.value = current.copy(
            isLoading = true,
            question = question,
            errorMessage = null,
            privacyDisclosed = true
        )

        viewModelScope.launch {
            val prep = repository.loadAndPrepareImageBase64(current.imageUri)
            if (prep == null) {
                _askAiState.value = _askAiState.value?.copy(
                    isLoading = false,
                    errorMessage = "Unable to read photograph from device storage."
                )
                return@launch
            }

            val (mimeType, base64) = prep
            val provider = currentAiService
            when (val result = provider.askAiAboutImage(mimeType, base64, question)) {
                is AiResult.Success -> {
                    _askAiState.value = _askAiState.value?.copy(
                        isLoading = false,
                        responseAnswer = result.data
                    )
                }
                is AiResult.Error -> {
                    _askAiState.value = _askAiState.value?.copy(
                        isLoading = false,
                        errorMessage = "[${provider.providerType.displayName}] ${result.message}"
                    )
                }
            }
        }
    }

    fun insertAiResponseIntoActiveEntry(observationText: String) {
        val currentText = _editorOriginalText.value
        val formattedObservation = "\n\n[Field Observation]\n$observationText"
        _editorOriginalText.value = (currentText + formattedObservation).trim()
        _askAiState.value = null
    }

    // --- Settings & Data Management ---

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAll()
            _snackbarMessage.value = "All local journal entries cleared"
            onComplete()
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
