package com.example.ui.screens.clinical

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.MediAssistViewModel
import com.example.utils.MatchedDoctorListing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DoctorFinderPane(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val isSearching by viewModel.isSearchingDoctors.collectAsState()
    val recommendation by viewModel.matchedDoctorRecommendation.collectAsState()
    val appointments by viewModel.doctorAppointments.collectAsState()
    val activePublicDoctors by viewModel.activePublicDoctors.collectAsState()
    val devAppConfig by viewModel.devAppConfig.collectAsState()

    var symptomQuery by remember { mutableStateOf("") }
    var bookingTarget by remember { mutableStateOf<MatchedDoctorListing?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.findSpecialistsNearby(context, symptomQuery.ifBlank { "General Health Checkup" })
    }

    val runSearch: () -> Unit = {
        val query = symptomQuery.ifBlank { "General Health Checkup" }
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine) {
            viewModel.findSpecialistsNearby(context, query)
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val quickSymptoms = listOf(
        tr("Chest Pain / BP", "છાતીમાં દુખાવો / BP", "सीने में दर्द / BP"),
        tr("Headache / Migraine", "માથાનો દુખાવો / ચક્કર", "सिरदर्द / माइग्रेन"),
        tr("Skin Rash / Allergy", "ચામડી પર ખંજવાળ / એલર્જી", "त्वचा एलर्जी / खुजली"),
        tr("Knee / Joint Pain", "ઘૂંટણ અને સાંધાનો દુખાવો", "घुटने और जोड़ों का दर्द"),
        tr("Cough / Breathing", "ઉધરસ અને શ્વાસની તકલીફ", "खांसी और सांस की दिक्कत")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            "Smart Doctor Finder & Booking",
                            "સ્માર્ટ ડૉક્ટર ફાઇન્ડર & ઓનલાઇન બુકિંગ",
                            "स्मार्ट डॉक्टर फ़ाइंडर और बुकिंग"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Matches your symptoms to the right specialist (Cardiologist, Neurologist...) & books nearby clinics via GPS",
                            "દર્દીની તકલીફ મુજબ યોગ્ય નિષ્ણાત ડૉક્ટર સજેસ્ટ કરી GPS દ્વારા નજીકની ઓનલાઇન એપોઇન્ટમેન્ટ બુક કરે છે",
                            "लक्षणों के आधार पर सही विशेषज्ञ सुझाएं और GPS से नज़दीकी अपॉइंटमेंट बुक करें"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Symptom Chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickSymptoms.forEach { label ->
                SuggestionChip(
                    onClick = { symptomQuery = label },
                    label = { Text(label) }
                )
            }
        }

        // Search Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = symptomQuery,
                    onValueChange = { symptomQuery = it },
                    label = { Text(tr("Describe Your Symptoms or Issue", "તમારી તકલીફ અથવા લક્ષણો લખો", "अपने लक्षण या समस्या लिखें")) },
                    placeholder = {
                        Text(
                            tr(
                                "e.g. Chest discomfort & high BP, or severe migraine headache...",
                                "દા.ત. છાતીમાં દુખાવો, માથાનો દુખાવો, ચામડીની એલર્જી...",
                                "जैसे: सीने में दर्द, माइग्रेन, घुटनों का दर्द..."
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("doctor_symptom_input")
                )

                Button(
                    onClick = runSearch,
                    enabled = !isSearching,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("doctor_find_btn")
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(tr("Match Specialist & Nearby Doctors", "નિષ્ણાત અને નજીકના ડૉક્ટર શોધો", "विशेषज्ञ और नज़दीकी डॉक्टर खोजें"))
                }
            }
        }

        // Public Doctors Published by Developer (Mobile Call & Booking)
        if (devAppConfig.publicDoctorConsultEnabled && activePublicDoctors.isNotEmpty()) {
            Text(
                text = tr(
                    "Verified Public Doctors (${activePublicDoctors.size})",
                    "પબ્લિક ડૉક્ટર્સ (કૉલ અને અપોઇન્ટમેન્ટ બુકિંગ)",
                    "पब्लिक डॉक्टर (कॉल और अपॉइंटमेंट बुकिंग)"
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            activePublicDoctors.forEachIndexed { idx, pubDoc ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().testTag("public_doctor_card_$idx")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pubDoc.doctorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${pubDoc.specialty} • ${pubDoc.clinicOrCity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("📞 ${pubDoc.mobileNumber} • ${pubDoc.consultationFee}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${pubDoc.mobileNumber}")))
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("public_doc_call_$idx")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tr("Call", "કૉલ", "कॉल"), fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    bookingTarget = MatchedDoctorListing(
                                        doctorName = pubDoc.doctorName,
                                        specialty = pubDoc.specialty,
                                        clinicName = pubDoc.clinicOrCity,
                                        distanceKm = 0.5,
                                        phoneNumber = pubDoc.mobileNumber,
                                        latitude = 0.0,
                                        longitude = 0.0,
                                        consultationFee = pubDoc.consultationFee,
                                        rating = 4.9
                                    )
                                },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("public_doc_book_$idx")
                            ) {
                                Text(tr("Book", "બુક", "बुक"), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Matched Specialist & Nearby Doctor Listings
        recommendation?.let { match ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SafeMint.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth().testTag("doctor_recommendation_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = tr("Recommended Specialist", "સૂચવેલ નિષ્ણાત ડૉક્ટર", "अनुशंसित विशेषज्ञ"),
                        style = MaterialTheme.typography.labelLarge,
                        color = SafeMint
                    )
                    Text(
                        text = match.localizedSpecialty,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = match.urgencyNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            match.doctorDirectory.forEachIndexed { idx, listing ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("doctor_card_$idx")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(listing.doctorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${listing.specialty} • ${listing.clinicName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "📍 ${String.format(Locale.US, "%.1f", listing.distanceKm)} km  •  ⭐ ${listing.rating}  •  ${listing.consultationFee}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val geo = Uri.parse("geo:${listing.latitude},${listing.longitude}?q=${Uri.encode(listing.clinicName)}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, geo))
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tr("Maps", "મેપ", "मैप"))
                            }
                            if (listing.phoneNumber.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${listing.phoneNumber}")))
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.weight(0.9f).height(48.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tr("Call", "કૉલ", "कॉल"))
                                }
                            }
                            Button(
                                onClick = { bookingTarget = listing },
                                modifier = Modifier.weight(1.25f).height(48.dp).testTag("doctor_book_btn_$idx")
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tr("Book", "બુક કરો", "बुक करें"))
                            }
                        }
                    }
                }
            }
        }

        // Confirmed Appointments List
        if (appointments.isNotEmpty()) {
            Text(
                text = tr(
                    "Confirmed Appointments (${appointments.size})",
                    "બુક કરેલ એપોઇન્ટમેન્ટ (${appointments.size})",
                    "बुक किए गए अपॉइंटमेंट (${appointments.size})"
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            appointments.forEach { appt ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("booked_appointment_card_${appt.id}")
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${appt.doctorName} (${appt.specialty})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${appt.clinicName} • ${appt.appointmentDate} ${appt.timeSlot}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Ref: ${appt.bookingRef} • ${appt.consultationMode} • ${appt.status}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SafeMint,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { viewModel.cancelDoctorAppointment(appt) }) {
                            Icon(Icons.Default.Delete, contentDescription = tr("Cancel", "રદ કરો", "रद्द करें"), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Appointment Booking Dialog
    bookingTarget?.let { doc ->
        val today = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }
        var selectedDate by remember { mutableStateOf(today) }
        var selectedSlot by remember { mutableStateOf("10:30 AM") }
        var selectedMode by remember { mutableStateOf("Online Video Consult") }

        AlertDialog(
            onDismissRequest = { bookingTarget = null },
            title = {
                Text(tr("Book Appointment", "એપોઇન્ટમેન્ટ બુક કરો", "अपॉइंटमेंट बुक करें"), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${doc.doctorName}\n${doc.clinicName} (${doc.specialty})", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        label = { Text(tr("Date", "તારીખ", "तारीख")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(tr("Select Time Slot", "સમય પસંદ કરો", "समय चुनें"), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        listOf("10:30 AM", "01:00 PM", "05:30 PM", "07:00 PM").forEach { slot ->
                            FilterChip(selected = selectedSlot == slot, onClick = { selectedSlot = slot }, label = { Text(slot) })
                        }
                    }
                    Text(tr("Consultation Mode", "પરામર્શ પ્રકાર", "परामर्श प्रकार"), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        listOf("Online Video Consult", "In-Clinic Visit").forEach { mode ->
                            FilterChip(selected = selectedMode == mode, onClick = { selectedMode = mode }, label = { Text(mode) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.bookDoctorAppointment(
                            listing = doc,
                            appointmentDate = selectedDate,
                            timeSlot = selectedSlot,
                            consultationMode = selectedMode,
                            symptomNote = symptomQuery
                        )
                        bookingTarget = null
                        Toast.makeText(
                            context,
                            trLang(selectedLanguage, "Appointment Confirmed!", "એપોઇન્ટમેન્ટ બુક થઈ ગઈ!", "अपॉइंटमेंट कन्फ़र्म हो गया!"),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.testTag("confirm_doctor_booking_btn")
                ) {
                    Text(tr("Confirm Booking", "કન્ફર્મ કરો", "कन्फ़र्म करें"))
                }
            },
            dismissButton = {
                TextButton(onClick = { bookingTarget = null }) {
                    Text(tr("Cancel", "રદ કરો", "रद्द करें"))
                }
            }
        )
    }
}
