package com.example

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ScannedReportEntity
import com.example.data.repository.TriageRepository
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import com.example.utils.MedicalReportOcrScanner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfLabReportTest {

    @Test
    fun `isPdfUri detects pdf filenames and ignores non-pdf images`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pdfUri = Uri.parse("file:///storage/emulated/0/Download/cbc_blood_report.PDF")
        val jpgUri = Uri.parse("file:///storage/emulated/0/Pictures/prescription.jpg")

        assertTrue(MedicalReportOcrScanner.isPdfUri(context, pdfUri))
        assertFalse(MedicalReportOcrScanner.isPdfUri(context, jpgUri))
    }

    @Test
    fun `extractTextFromPdfUri returns failure gracefully for missing or invalid pdf`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val missingUri = Uri.fromFile(File(context.cacheDir, "does_not_exist.pdf"))

        val result = MedicalReportOcrScanner.extractTextFromPdfUri(context, missingUri)
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull()?.message)
    }

    @Test
    fun `pdf lab report explanation and persistence work cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = TriageRepository(db.triageDao())
            val explanation = repo.explainMedicalReportOrPrescription(
                extractedText = "Hemoglobin: 13.8 g/dL\nWBC: 7200 /uL\nPlatelets: 240,000 /uL",
                reportType = "Lab Report (PDF)",
                language = "Gujarati"
            )
            assertTrue(explanation.isSuccess)
            val body = explanation.getOrNull().orEmpty()
            assertTrue(body.isNotBlank())

            repo.saveScannedReport(
                ScannedReportEntity(
                    title = "Lab Report (PDF) • blood_test.pdf",
                    extractedText = "Hemoglobin: 13.8 g/dL",
                    aiExplanation = body,
                    imageUri = "content://downloads/blood_test.pdf",
                    reportType = "Lab Report (PDF)"
                )
            )

            val saved = repo.allScannedReports.first()
            assertEquals(1, saved.size)
            assertEquals("Lab Report (PDF)", saved[0].reportType)
        } finally {
            db.close()
        }
    }

    @Test
    fun `opening REPORT_SCANNER in MainActivity composes without crashing`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        val activity = controller.get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val viewModel = ViewModelProvider(activity)[MediAssistViewModel::class.java]
        viewModel.setScreen(AppScreen.REPORT_SCANNER)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(AppScreen.REPORT_SCANNER, viewModel.currentScreen.value)

        viewModel.setScreen(AppScreen.TRIAGE_CHAT)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        controller.pause().stop().destroy()
    }
}
