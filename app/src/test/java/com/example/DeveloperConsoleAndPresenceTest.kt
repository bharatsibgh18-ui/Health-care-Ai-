package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DeveloperUserPresenceSummary
import com.example.data.local.PublicDoctorEntity
import com.example.data.repository.TriageRepository
import com.example.ui.screens.formatOnlineDuration
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import com.example.utils.AuthSecurityHelper
import android.os.Looper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeveloperConsoleAndPresenceTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: TriageRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AuthSecurityHelper.clearSession(context)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = TriageRepository(db.triageDao())
    }

    @After
    fun tearDown() {
        AuthSecurityHelper.clearSession(context)
        db.close()
    }

    @Test
    fun `presence projection strictly exposes only username and online activity without sensitive fields`() = runBlocking {
        repo.registerCustomUserAccount("meera_sayla", "Meera Patel", "Secret@2026")
        repo.recordUserHeartbeat(
            username = "meera_sayla",
            authMethod = "PASSWORD",
            deltaSeconds = 95L,
            isOnline = true
        )
        repo.recordUserHeartbeat(
            username = "meera_sayla",
            authMethod = "PASSWORD",
            deltaSeconds = 30L,
            isOnline = true
        )

        val presenceList = repo.developerUserPresenceList.first()
        val meera = presenceList.firstOrNull { it.username == "meera_sayla" }
        assertNotNull(meera)
        assertEquals(125L, meera!!.totalOnlineSeconds)
        assertTrue(meera.isCurrentlyOnline)

        // Mark offline updates flag while preserving accumulated online duration
        repo.markUserOffline("meera_sayla")
        val updated = repo.developerUserPresenceList.first().first { it.username == "meera_sayla" }
        assertFalse(updated.isCurrentlyOnline)
        assertEquals(125L, updated.totalOnlineSeconds)

        // Reflection check: DeveloperUserPresenceSummary exposes zero password/salt/hash/phone/health fields
        val fieldNames = DeveloperUserPresenceSummary::class.java.declaredFields.map { it.name.lowercase() }
        assertTrue(fieldNames.contains("username"))
        assertTrue(fieldNames.contains("totalonlineseconds"))
        assertTrue(fieldNames.contains("lastonlineat"))
        assertFalse(fieldNames.any { it.contains("password") || it.contains("salt") || it.contains("hash") || it.contains("phone") || it.contains("symptom") })
    }

    @Test
    fun `public doctors and manual app config upgrades require developer login SHAILESH`() = runBlocking {
        val app = context.applicationContext as Application
        val viewModel = MediAssistViewModel(app)

        // Non-developer cannot publish public doctors or live app upgrades
        assertFalse(
            viewModel.savePublicDoctor(
                doctorName = "Dr. Unauthorized",
                specialty = "General Physician",
                clinicOrCity = "City Clinic",
                mobileNumber = "9999988888",
                consultationFee = "₹300",
                videoCallEnabled = true,
                videoCallRoomUrl = ""
            )
        )
        assertFalse(
            viewModel.saveDeveloperAppConfig(
                appBannerNotice = "Unauthorized Banner",
                publicDoctorConsultEnabled = true,
                videoCallFeatureEnabled = true,
                medicineAdvisorFeatureEnabled = true,
                emergencyHelplineOverride = "108"
            )
        )

        // Authenticate strictly as Developer SHAILESH
        assertTrue(viewModel.loginAsDeveloper("SHAILESH", "Shailesh@1736"))

        assertTrue(
            viewModel.savePublicDoctor(
                doctorName = "Dr. R. Chauhan (MD)",
                specialty = "Cardiologist",
                clinicOrCity = "Sayla Care Hospital",
                mobileNumber = "9825011223",
                consultationFee = "₹400",
                videoCallEnabled = true,
                videoCallRoomUrl = "https://meet.jit.si/SaylaCareDrChauhan"
            )
        )
        assertTrue(
            viewModel.saveDeveloperAppConfig(
                appBannerNotice = "Tele-Consultation Camp Open 6 PM - 9 PM",
                publicDoctorConsultEnabled = true,
                videoCallFeatureEnabled = true,
                medicineAdvisorFeatureEnabled = true,
                emergencyHelplineOverride = "108"
            )
        )

        val appDbDao = AppDatabase.getDatabase(app).triageDao()
        var chauhan: PublicDoctorEntity? = null
        val deadlineMs = System.currentTimeMillis() + 5000L
        while (System.currentTimeMillis() < deadlineMs && chauhan == null) {
            shadowOf(Looper.getMainLooper()).idle()
            chauhan = appDbDao.getActivePublicDoctors().first()
                .firstOrNull { it.doctorName == "Dr. R. Chauhan (MD)" }
            if (chauhan == null) {
                kotlinx.coroutines.delay(50)
            }
        }
        assertNotNull(chauhan)
        assertFalse(chauhan!!.videoCallEnabled)
        assertEquals("", chauhan.videoCallRoomUrl)

        // Verify direct repository CRUD for PublicDoctorEntity & ensure videoCallEnabled defaults to false

        val doc = PublicDoctorEntity(
            doctorName = "Dr. H. Jadeja",
            specialty = "Neurologist",
            clinicOrCity = "Surendranagar",
            mobileNumber = "9876501234",
            consultationFee = "₹500"
        )
        repo.upsertPublicDoctor(doc)
        val activeDocs = repo.activePublicDoctors.first()
        val jadeja = activeDocs.firstOrNull { it.doctorName == "Dr. H. Jadeja" && it.mobileNumber == "9876501234" }
        assertNotNull(jadeja)
        assertFalse(jadeja!!.videoCallEnabled)
    }

    @Test
    fun `developer can publish public app upgrade with download link and clear it while non-developers are blocked`() = runBlocking {
        val app = context.applicationContext as Application
        val viewModel = MediAssistViewModel(app)

        // Non-developer cannot publish public upgrade
        assertFalse(
            viewModel.publishPublicAppUpgrade(
                targetVersion = "18.0",
                title = "Unauthorized Update",
                message = "Should fail",
                downloadUrl = "https://example.com/app.apk",
                mandatory = false
            )
        )

        // Login as Developer SHAILESH
        assertTrue(viewModel.loginAsDeveloper("SHAILESH", "Shailesh@1736"))

        // Publish Public Upgrade
        assertTrue(
            viewModel.publishPublicAppUpgrade(
                targetVersion = "18.0",
                title = "Health Care AI v18.0 Live Update",
                message = "5s splash animation, all 21 languages everywhere & faster AI triage.",
                downloadUrl = "github.com/modelschoolsayla/healthcare-ai/releases/latest",
                mandatory = true
            )
        )

        val appDbDao = AppDatabase.getDatabase(app).triageDao()
        var savedCfg = appDbDao.getDevAppConfigOnce()
        val deadlineMs = System.currentTimeMillis() + 5000L
        while (System.currentTimeMillis() < deadlineMs && savedCfg?.upgradePublished != true) {
            shadowOf(Looper.getMainLooper()).idle()
            savedCfg = appDbDao.getDevAppConfigOnce()
            if (savedCfg?.upgradePublished != true) kotlinx.coroutines.delay(50)
        }
        assertNotNull(savedCfg)
        assertTrue(savedCfg!!.upgradePublished)
        assertEquals("18.0", savedCfg.upgradeTargetVersion)
        assertEquals("https://github.com/modelschoolsayla/healthcare-ai/releases/latest", savedCfg.upgradeDownloadUrl)
        assertTrue(savedCfg.upgradeMandatory)
        assertEquals(5, savedCfg.splashDurationSeconds)

        // Clear published upgrade
        assertTrue(viewModel.clearPublishedAppUpgrade())
        val clearDeadline = System.currentTimeMillis() + 5000L
        while (System.currentTimeMillis() < clearDeadline && savedCfg?.upgradePublished != false) {
            shadowOf(Looper.getMainLooper()).idle()
            savedCfg = appDbDao.getDevAppConfigOnce()
            if (savedCfg?.upgradePublished != false) kotlinx.coroutines.delay(50)
        }
        assertFalse(savedCfg!!.upgradePublished)
    }

    @Test
    fun `online duration helper formats seconds minutes and hours cleanly`() {
        assertEquals("45s", formatOnlineDuration(45))
        assertEquals("2m 5s", formatOnlineDuration(125))
        assertEquals("1h 5m", formatOnlineDuration(3900))
    }
}
