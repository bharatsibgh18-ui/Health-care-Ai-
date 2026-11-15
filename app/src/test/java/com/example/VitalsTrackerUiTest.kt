package com.example

import android.content.Context
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.VitalLogEntity
import com.example.data.repository.TriageRepository
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VitalsTrackerUiTest {

    @Test
    fun `opening VITALS_TRACKER in MainActivity composes and lays out without crashing`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        val activity = controller.get()
        assertNotNull(activity)

        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val viewModel = ViewModelProvider(activity)[MediAssistViewModel::class.java]
        viewModel.setScreen(AppScreen.VITALS_TRACKER)
        assertEquals(AppScreen.VITALS_TRACKER, viewModel.currentScreen.value)

        // Drive Choreographer / Compose frames for VitalsTrackerScreen
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Navigate back to home triage screen cleanly
        viewModel.setScreen(AppScreen.TRIAGE_CHAT)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(AppScreen.TRIAGE_CHAT, viewModel.currentScreen.value)

        controller.pause().stop().destroy()
    }

    @Test
    fun `vital logs insert query and delete cleanly in room database`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = TriageRepository(db.triageDao())
            val bpLog = VitalLogEntity(
                vitalType = "Blood Pressure",
                primaryValue = 120.0,
                secondaryValue = 80.0,
                unit = "mmHg",
                status = "Normal",
                notes = "Morning reading"
            )
            val id = repo.saveVitalLog(bpLog)
            val all = repo.allVitalLogs.first()
            assertEquals(1, all.size)
            assertEquals("Blood Pressure", all[0].vitalType)
            assertEquals(120.0, all[0].primaryValue, 0.01)
            assertEquals(80.0, all[0].secondaryValue, 0.01)

            repo.deleteVitalLog(all[0].copy(id = id))
            val afterDelete = repo.allVitalLogs.first()
            assertEquals(0, afterDelete.size)
        } finally {
            db.close()
        }
    }
}
