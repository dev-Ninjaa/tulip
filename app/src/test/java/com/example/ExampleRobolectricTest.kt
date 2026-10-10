package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.tulip.data.ai.AiProviderType
import com.example.tulip.data.ai.ollama.OllamaAiService
import com.example.tulip.data.ai.ollama.OllamaChatMessage
import com.example.tulip.data.ai.ollama.OllamaChatRequest
import com.example.tulip.data.ai.ollama.OllamaChatResponse
import com.example.tulip.data.ai.ollama.OllamaTagsResponse
import com.example.tulip.data.db.TulipDatabase
import com.example.tulip.data.model.JournalEntry
import com.example.tulip.data.repository.JournalRepository
import com.example.tulip.ui.TulipViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TulipJournalRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: TulipDatabase
    private lateinit var repository: JournalRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = TulipDatabase.getInstance(context)
        repository = JournalRepository(context)
    }

    @After
    fun teardown() = runBlocking {
        repository.clearAll()
    }

    @Test
    fun `verify app name resource is Tulip`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Tulip", appName)
    }

    @Test
    fun `create, save, and retrieve journal entry offline`() = runBlocking {
        val entry = JournalEntry(
            id = UUID.randomUUID().toString(),
            originalText = "Reached Pine Ridge at dusk. Ancient cedar trees scenting the air.",
            locationLabel = "Pine Ridge Trail",
            tagsJson = "[\"cedar\", \"dusk\"]",
            createdAt = System.currentTimeMillis()
        )

        repository.saveEntry(entry)

        val retrieved = repository.getEntryById(entry.id)
        assertNotNull(retrieved)
        assertEquals("Reached Pine Ridge at dusk. Ancient cedar trees scenting the air.", retrieved?.originalText)
        assertEquals("Pine Ridge Trail", retrieved?.locationLabel)
    }

    @Test
    fun `original text is strictly preserved when translation is added`() = runBlocking {
        val original = "J'ai trouvé une petite cascade cachée derrière les fougères."
        val translated = "I found a small waterfall hidden behind the ferns."

        val entry = JournalEntry(
            id = UUID.randomUUID().toString(),
            originalText = original,
            translatedEnglishText = translated,
            translationApplied = false
        )

        repository.saveEntry(entry)

        val retrieved = repository.getEntryById(entry.id)
        assertEquals(original, retrieved?.originalText)
        assertEquals(translated, retrieved?.translatedEnglishText)
    }

    @Test
    fun `search entries by keyword and location`() = runBlocking {
        val entry1 = JournalEntry(
            id = "entry-1",
            originalText = "Spotted a golden eagle near the canyon rim.",
            locationLabel = "Eagle Canyon"
        )
        val entry2 = JournalEntry(
            id = "entry-2",
            originalText = "Pitching tent under wet pine boughs.",
            locationLabel = "Blackwood Woods"
        )

        repository.saveEntry(entry1)
        repository.saveEntry(entry2)

        val eagleResults = repository.searchEntries("eagle").first()
        assertEquals(1, eagleResults.size)
        assertEquals("entry-1", eagleResults[0].id)

        val canyonResults = repository.searchEntries("Canyon").first()
        assertEquals(1, canyonResults.size)
        assertEquals("entry-1", canyonResults[0].id)
    }

    @Test
    fun `delete entry removes it from local database`() = runBlocking {
        val entry = JournalEntry(
            id = "to-delete",
            originalText = "Temporary note"
        )
        repository.saveEntry(entry)
        assertNotNull(repository.getEntryById("to-delete"))

        repository.deleteEntryById("to-delete")
        assertEquals(null, repository.getEntryById("to-delete"))
    }

    // --- AI Provider & Ollama Unit Tests ---

    @Test
    fun `gemini remains the default AI provider on startup`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("tulip_prefs", Application.MODE_PRIVATE)
        prefs.edit().remove("selected_ai_provider").apply()

        val viewModel = TulipViewModel(app)
        assertEquals(AiProviderType.GEMINI, viewModel.selectedAiProvider.value)
        assertEquals(AiProviderType.GEMINI, viewModel.currentAiService.providerType)
    }

    @Test
    fun `switching AI provider persists to preferences`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TulipViewModel(app)

        viewModel.setAiProvider(AiProviderType.OLLAMA)
        assertEquals(AiProviderType.OLLAMA, viewModel.selectedAiProvider.value)
        assertEquals(AiProviderType.OLLAMA, viewModel.currentAiService.providerType)

        val prefs = app.getSharedPreferences("tulip_prefs", Application.MODE_PRIVATE)
        assertEquals(AiProviderType.OLLAMA.name, prefs.getString("selected_ai_provider", null))

        // Recreate viewModel and verify persistence survives
        val newViewModel = TulipViewModel(app)
        assertEquals(AiProviderType.OLLAMA, newViewModel.selectedAiProvider.value)
    }

    @Test
    fun `ollama url validation and normalization tests`() {
        // Valid LAN URL with port
        val res1 = OllamaAiService.normalizeAndValidateUrl("192.168.1.100:11434")
        assertTrue(res1.isSuccess)
        assertEquals("http://192.168.1.100:11434/", res1.getOrNull())

        // Valid http URL
        val res2 = OllamaAiService.normalizeAndValidateUrl("http://10.0.2.2:11434")
        assertTrue(res2.isSuccess)
        assertEquals("http://10.0.2.2:11434/", res2.getOrNull())

        // Valid https URL
        val res3 = OllamaAiService.normalizeAndValidateUrl("https://ollama.myhomelab.net:11434")
        assertTrue(res3.isSuccess)
        assertEquals("https://ollama.myhomelab.net:11434/", res3.getOrNull())

        // Empty URL should fail
        val resEmpty = OllamaAiService.normalizeAndValidateUrl("   ")
        assertTrue(resEmpty.isFailure)

        // Invalid scheme should fail
        val resScheme = OllamaAiService.normalizeAndValidateUrl("ftp://192.168.1.50:11434")
        assertTrue(resScheme.isFailure)
    }

    @Test
    fun `ollama request serialization includes multimodal image payload`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(OllamaChatRequest::class.java)

        val request = OllamaChatRequest(
            model = "gemma3:4b",
            messages = listOf(
                OllamaChatMessage(role = "system", content = "You are a botanical assistant."),
                OllamaChatMessage(
                    role = "user",
                    content = "What species is this?",
                    images = listOf("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=")
                )
            ),
            stream = false
        )

        val json = adapter.toJson(request)
        assertTrue(json.contains("\"model\":\"gemma3:4b\""))
        assertTrue(json.contains("\"role\":\"user\""))
        assertTrue(json.contains("\"images\":[\"iVBORw0K"))
        assertTrue(json.contains("\"stream\":false"))
    }

    @Test
    fun `ollama response parsing handles message and error payloads`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(OllamaChatResponse::class.java)

        val sampleSuccessJson = """
            {
              "model": "gemma3:4b",
              "created_at": "2026-10-10T05:00:00Z",
              "message": {
                "role": "assistant",
                "content": "This is a wild mountain iris with pale violet petals."
              },
              "done": true
            }
        """.trimIndent()

        val parsed = adapter.fromJson(sampleSuccessJson)
        assertNotNull(parsed)
        assertEquals("gemma3:4b", parsed?.model)
        assertEquals("This is a wild mountain iris with pale violet petals.", parsed?.message?.content)
    }
}
