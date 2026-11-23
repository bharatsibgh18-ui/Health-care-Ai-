package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.GeminiApiService
import com.example.data.api.GeminiCandidate
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerateContentRequest
import com.example.data.api.GeminiGenerateContentResponse
import com.example.data.api.GeminiPart
import com.example.data.api.SlowNetworkException
import com.example.data.local.AppDatabase
import com.example.data.repository.TriageRepository
import com.example.ui.viewmodel.MediAssistViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.SocketTimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SlowNetworkAndVoiceTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `sendTriageQuery retries transient slow internet timeout and returns real AI reply`() = runBlocking {
        var calls = 0
        val calledModels = mutableListOf<String>()
        val flakyService = object : GeminiApiService {
            override suspend fun generateContent(
                model: String,
                apiKey: String,
                request: GeminiGenerateContentRequest
            ): GeminiGenerateContentResponse {
                calls++
                calledModels.add(model)
                if (calls == 1) {
                    throw SocketTimeoutException("Simulated slow 3G timeout")
                }
                return GeminiGenerateContentResponse(
                    candidates = listOf(
                        GeminiCandidate(
                            content = GeminiContent(
                                parts = listOf(GeminiPart(text = "Accurate AI Answer after retry")),
                                role = "model"
                            )
                        )
                    )
                )
            }
        }

        val repo = TriageRepository(
            triageDao = db.triageDao(),
            geminiService = flakyService,
            apiKeyOverride = "test-live-api-key"
        )
        val res = repo.sendTriageQuery("મને તાવ અને માથાનો દુખાવો છે શું કરવું?", emptyList(), null)
        assertTrue(res.isSuccess)
        assertEquals("Accurate AI Answer after retry", res.getOrNull())
        assertEquals(2, calls)
        assertFalse(calledModels.contains("gemini-3.8-flash"))
    }

    @Test
    fun `sendTriageQuery falls back to valid gemini-2-5-flash never invalid gemini-3-8-flash`() = runBlocking {
        val calledModels = mutableListOf<String>()
        val modelSwitchingService = object : GeminiApiService {
            override suspend fun generateContent(
                model: String,
                apiKey: String,
                request: GeminiGenerateContentRequest
            ): GeminiGenerateContentResponse {
                calledModels.add(model)
                if (model == "gemini-3.1-flash-lite-preview") {
                    throw SocketTimeoutException("Primary model timed out")
                }
                return GeminiGenerateContentResponse(
                    candidates = listOf(
                        GeminiCandidate(
                            content = GeminiContent(
                                parts = listOf(GeminiPart(text = "Verified response from $model")),
                                role = "model"
                            )
                        )
                    )
                )
            }
        }

        val repo = TriageRepository(
            triageDao = db.triageDao(),
            geminiService = modelSwitchingService,
            apiKeyOverride = "test-live-api-key"
        )
        val res = repo.sendTriageQuery("મારે ગળામાં દુખે છે", emptyList(), null)
        assertTrue(res.isSuccess)
        assertEquals("Verified response from gemini-2.5-flash", res.getOrNull())
        assertTrue(calledModels.contains("gemini-2.5-flash"))
        assertFalse(calledModels.contains("gemini-3.8-flash"))
    }

    @Test
    fun `persistent slow internet failure does NOT return wrong guessed fallback for Q&A or Video Call`() = runBlocking {
        val alwaysTimeoutService = object : GeminiApiService {
            override suspend fun generateContent(
                model: String,
                apiKey: String,
                request: GeminiGenerateContentRequest
            ): GeminiGenerateContentResponse {
                throw SocketTimeoutException("Slow mobile data timed out")
            }
        }

        val repo = TriageRepository(
            triageDao = db.triageDao(),
            geminiService = alwaysTimeoutService,
            apiKeyOverride = "test-live-api-key"
        )

        val chatResult = repo.sendTriageQuery("આંખ લાલ થઈ ગઈ છે અને બળે છે શું કરવું?", emptyList(), null)
        assertTrue(chatResult.isFailure)
        assertTrue(chatResult.exceptionOrNull() is SlowNetworkException)

        val videoResult = repo.sendVideoConsultationTurn(
            spokenMessage = "મારા પગમાં સોજો છે",
            recentCallTurns = emptyList(),
            userProfile = null,
            language = "Gujarati"
        )
        assertTrue(videoResult.isFailure)
        assertTrue(videoResult.exceptionOrNull() is SlowNetworkException)

        // Emergency red-flag queries still trigger urgent 108/112 alert immediately even offline
        val emergencyResult = repo.sendTriageQuery("severe crushing chest pain", emptyList(), null)
        assertTrue(emergencyResult.isSuccess)
        assertTrue((emergencyResult.getOrNull() ?: "").contains("EMERGENCY RED FLAG ALERT"))
    }

    @Test
    fun `word boundary fallback does not misdetect English headache check as Gujarati`() = runBlocking {
        val repo = TriageRepository(
            triageDao = db.triageDao(),
            apiKeyOverride = ""
        )
        val res = repo.sendTriageQuery("I have a mild headache check needed", emptyList(), null)
        assertTrue(res.isSuccess)
        val text = res.getOrNull().orEmpty()
        assertFalse(text.contains("નમસ્તે"))
        assertTrue(text.contains("Headache") || text.contains("self-care", ignoreCase = true) || text.contains("Recommended"))
    }

    @Test
    fun `video call greeting initializes hands-free mode without mic press prompt`() {
        val app = context.applicationContext as Application
        val viewModel = MediAssistViewModel(app)
        viewModel.setSelectedLanguage("Gujarati")
        viewModel.startVideoCallGreeting()

        assertTrue(viewModel.videoCallAutoListen.value)
        assertEquals(1, viewModel.videoCallTurns.value.size)
        val greeting = viewModel.videoCallTurns.value.first().text
        assertTrue(greeting.contains("માઇક દબાવ્યા વગર"))
    }
}
