package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import com.example.data.local.UserProfileEntity
import com.example.data.repository.TriageRepository
import com.example.ui.viewmodel.MediAssistViewModel
import com.example.utils.AuthRole
import com.example.utils.AuthSecurityHelper
import com.example.utils.MedicineAdvisorEngine
import com.example.utils.SeverityGuidanceTier
import com.example.utils.WaterReminderNotificationHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthAndMedicineAdvisorTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: TriageRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        try {
            WorkManager.initialize(context, Configuration.Builder().build())
        } catch (_: IllegalStateException) {
            // Already initialized in Robolectric
        }
        AuthSecurityHelper.clearSession(context)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = TriageRepository(db.triageDao())
    }

    @After
    fun teardown() {
        AuthSecurityHelper.clearSession(context)
        db.close()
    }

    @Test
    fun `developer login accepts only exact SHAILESH and Shailesh@1736 credentials`() {
        assertTrue(AuthSecurityHelper.verifyDeveloperCredentials("SHAILESH", "Shailesh@1736"))
        assertTrue(AuthSecurityHelper.verifyDeveloperCredentials("  SHAILESH  ", "Shailesh@1736"))

        // Strict case-sensitive & exact match checks
        assertFalse(AuthSecurityHelper.verifyDeveloperCredentials("shailesh", "Shailesh@1736"))
        assertFalse(AuthSecurityHelper.verifyDeveloperCredentials("SHAILESH", "shailesh@1736"))
        assertFalse(AuthSecurityHelper.verifyDeveloperCredentials("SHAILESH", "WrongPass123"))
        assertFalse(AuthSecurityHelper.verifyDeveloperCredentials("admin", "Shailesh@1736"))
        assertFalse(AuthSecurityHelper.verifyDeveloperCredentials("", ""))
    }

    @Test
    fun `custom user sign up stores salted hash and login only succeeds with created username and password`() = runBlocking {
        // Cannot register reserved developer username
        val reservedResult = repo.registerCustomUserAccount("SHAILESH", "Fake Dev", "Pass@1234")
        assertTrue(reservedResult.isFailure)

        // Sign up a valid normal user
        val signUpResult = repo.registerCustomUserAccount("kiran_patel", "Kiran Patel", "Health@2026")
        assertTrue(signUpResult.isSuccess)
        val savedAccount = signUpResult.getOrThrow()

        // Verify password is salted SHA-256 hash, never plaintext
        assertNotEquals("Health@2026", savedAccount.passwordHash)
        assertTrue(savedAccount.passwordSalt.isNotBlank())
        assertTrue(savedAccount.passwordHash.length == 64)

        // Wrong password must fail
        val wrongPasswordLogin = repo.loginCustomUserAccount("kiran_patel", "WrongPass999")
        assertTrue(wrongPasswordLogin.isFailure)

        // Unknown username must fail
        val unknownUserLogin = repo.loginCustomUserAccount("unknown_user", "Health@2026")
        assertTrue(unknownUserLogin.isFailure)

        // Exact created username + password must succeed
        val validLogin = repo.loginCustomUserAccount("kiran_patel", "Health@2026")
        assertTrue(validLogin.isSuccess)
        assertEquals("Kiran Patel", validLogin.getOrThrow().displayName)
    }

    @Test
    fun `mobile OTP generation and ViewModel OTP verification enforce matching 6-digit code`() {
        val app = context.applicationContext as Application
        val viewModel = MediAssistViewModel(app)

        // Developer login enforcement via ViewModel
        assertFalse(viewModel.loginAsDeveloper("shailesh", "Shailesh@1736"))
        assertFalse(viewModel.authSession.value.isDeveloper)

        assertTrue(viewModel.loginAsDeveloper("SHAILESH", "Shailesh@1736"))
        assertTrue(viewModel.authSession.value.isLoggedIn)
        assertEquals(AuthRole.DEVELOPER, viewModel.authSession.value.role)
        assertEquals("SHAILESH", viewModel.authSession.value.username)

        viewModel.logoutAccount()
        assertFalse(viewModel.authSession.value.isLoggedIn)

        // Mobile OTP generation & validation
        val otp = viewModel.sendLoginOtp("9876543210")
        assertNotNull(otp)
        assertEquals(6, otp!!.length)
        assertTrue(otp.all { it.isDigit() })

        // Incorrect OTP must be rejected
        viewModel.verifyLoginOtp("9876543210", "000000")
        assertNotNull(viewModel.authError.value)
        assertFalse(viewModel.authSession.value.isLoggedIn)

        // Verify OTP account persistence in Room directly
        val otpAccount = runBlocking { repo.findOrCreateOtpAccount("+91 9876543210") }
        assertTrue(otpAccount.isSuccess)
        assertEquals("9876543210", otpAccount.getOrThrow().phoneNumber)
        assertEquals("MOBILE_OTP", otpAccount.getOrThrow().authMethod)
    }

    @Test
    fun `medicine advisor recommends no unnecessary medicine for mild severity and lists medicines for moderate or severe illness`() {
        val sampleProfile = UserProfileEntity(
            age = 29,
            allergies = "Sulfa",
            chronicConditions = "None"
        )

        // Mild symptom (severity 2/10, no red flags) -> advise doctor / rest, NO medicine needed
        val mildResult = MedicineAdvisorEngine.evaluateLocally(
            symptomOrIllness = "Mild headache and tiredness",
            severity = 2,
            duration = "Since morning",
            language = "English",
            profile = sampleProfile,
            resolveImagesOnline = false
        )
        assertEquals(SeverityGuidanceTier.MILD_NO_MED_NEEDED, mildResult.tier)
        assertTrue(mildResult.medicines.isEmpty())
        assertTrue(mildResult.homeCareSteps.isNotEmpty())
        assertTrue(mildResult.clinicalSummary.contains("doctor", ignoreCase = true))
        assertNotNull(mildResult.allergyAlert)

        // Moderate symptom (severity 6/10) -> suggests matching medicines + online photo search URL
        val moderateResult = MedicineAdvisorEngine.evaluateLocally(
            symptomOrIllness = "Fever and acidity",
            severity = 6,
            duration = "2 days",
            language = "English",
            profile = sampleProfile,
            resolveImagesOnline = false
        )
        assertEquals(SeverityGuidanceTier.MODERATE_SUPPORTIVE_CARE, moderateResult.tier)
        assertTrue(moderateResult.medicines.any { it.name.contains("Paracetamol", ignoreCase = true) })
        assertTrue(moderateResult.medicines.any { it.name.contains("Pantoprazole", ignoreCase = true) })
        assertTrue(moderateResult.medicines.all { it.onlinePhotoSearchUrl.startsWith("https://") })

        // Red-flag symptom -> severe doctor urgent even if slider lower
        val urgentResult = MedicineAdvisorEngine.evaluateLocally(
            symptomOrIllness = "Severe chest pain and trouble breathing",
            severity = 5,
            duration = "1 hour",
            language = "English",
            profile = sampleProfile,
            resolveImagesOnline = false
        )
        assertEquals(SeverityGuidanceTier.SEVERE_DOCTOR_URGENT, urgentResult.tier)
    }

    @Test
    fun `water reminder outside-app interval persists and schedules cleanly`() {
        WaterReminderNotificationHelper.scheduleWaterReminder(context, 3)
        assertEquals(3, WaterReminderNotificationHelper.getSavedIntervalHours(context))
        assertTrue(WaterReminderNotificationHelper.isWaterReminderEnabled(context))
        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("water_hydration_periodic_work")
            .get()
        assertTrue(infos.isNotEmpty())
    }

    @Test
    fun `mobile OTP verify, custom signup, and login all advance currentScreen out of AUTH_LOGIN`() {
        val app = context.applicationContext as Application
        val viewModel = MediAssistViewModel(app)

        // 1. Mobile OTP verify when stuck on AUTH_LOGIN must log in AND advance to TRIAGE_CHAT
        viewModel.setScreen(com.example.ui.viewmodel.AppScreen.AUTH_LOGIN)
        val otp = viewModel.sendLoginOtp("+91 9876501234")!!
        var otpCallbackFired = false
        viewModel.verifyLoginOtp("9876501234", otp) { otpCallbackFired = true }

        val deadline = System.currentTimeMillis() + 5000L
        while (!viewModel.authSession.value.isLoggedIn && System.currentTimeMillis() < deadline) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(25)
        }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        assertTrue(viewModel.authSession.value.isLoggedIn)
        assertTrue(otpCallbackFired)
        assertEquals(com.example.ui.viewmodel.AppScreen.TRIAGE_CHAT, viewModel.currentScreen.value)

        // 2. Logout then Sign Up from AUTH_LOGIN must create unique user & open TRIAGE_CHAT
        viewModel.logoutAccount()
        viewModel.setScreen(com.example.ui.viewmodel.AppScreen.AUTH_LOGIN)
        val uniqueUser = "patient_${System.currentTimeMillis()}"
        var signupCallbackFired = false
        viewModel.registerWithUsernamePassword(
            username = uniqueUser,
            displayName = "Patient Demo",
            password = "Password@1",
            confirmPassword = "Password@1"
        ) { signupCallbackFired = true }

        val deadline2 = System.currentTimeMillis() + 5000L
        while (!viewModel.authSession.value.isLoggedIn && System.currentTimeMillis() < deadline2) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(25)
        }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        assertTrue(viewModel.authSession.value.isLoggedIn)
        assertTrue(signupCallbackFired)
        assertEquals(uniqueUser, viewModel.authSession.value.username)
        assertEquals(com.example.ui.viewmodel.AppScreen.TRIAGE_CHAT, viewModel.currentScreen.value)

        // 3. Logout then Password Login (even with mixed case) must open TRIAGE_CHAT
        viewModel.logoutAccount()
        viewModel.setScreen(com.example.ui.viewmodel.AppScreen.AUTH_LOGIN)
        var loginCallbackFired = false
        viewModel.loginWithUsernamePassword(
            username = uniqueUser.uppercase(),
            password = "Password@1"
        ) { loginCallbackFired = true }

        val deadline3 = System.currentTimeMillis() + 5000L
        while (!viewModel.authSession.value.isLoggedIn && System.currentTimeMillis() < deadline3) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(25)
        }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        assertTrue(viewModel.authSession.value.isLoggedIn)
        assertTrue(loginCallbackFired)
        assertEquals(com.example.ui.viewmodel.AppScreen.TRIAGE_CHAT, viewModel.currentScreen.value)
    }
}
