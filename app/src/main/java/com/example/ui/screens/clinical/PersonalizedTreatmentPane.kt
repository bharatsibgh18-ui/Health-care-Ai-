package com.example.ui.screens.clinical

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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PersonalizedTreatmentPane(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val vitalLogs by viewModel.vitalLogs.collectAsState()
    val scannedReports by viewModel.scannedReports.collectAsState()
    val isGenerating by viewModel.isGeneratingTreatmentPlan.collectAsState()
    val activePlan by viewModel.activeTreatmentPlan.collectAsState()
    val savedPlans by viewModel.treatmentPlans.collectAsState()

    val prefilledLabs = remember(vitalLogs, scannedReports) {
        val bp = vitalLogs.firstOrNull { it.vitalType.equals("Blood Pressure", ignoreCase = true) }
        val sugar = vitalLogs.firstOrNull { it.vitalType.equals("Blood Sugar", ignoreCase = true) }
        val latestReport = scannedReports.firstOrNull()?.title
        listOfNotNull(
            bp?.let { "BP ${it.primaryValue.toInt()}/${it.secondaryValue.toInt()} mmHg" },
            sugar?.let { "Sugar ${it.primaryValue.toInt()} mg/dL" },
            latestReport?.let { "Report: $it" }
        ).joinToString(", ")
    }

    var targetGoal by remember { mutableStateOf("") }
    var geneticsInput by remember { mutableStateOf("") }
    var lifestyleInput by remember { mutableStateOf("") }
    var labInput by remember(prefilledLabs) { mutableStateOf(prefilledLabs) }

    val geneticPresets = listOf(
        "Family Diabetes Risk" to tr("Family Diabetes", "પારિવારિક ડાયાબિટીસ", "पारिवारिक शुगर"),
        "Family Cardiac/BP" to tr("Family Cardiac/BP", "પારિવારિક BP/હૃદય", "पारिवारिक BP/हृदय"),
        "Thalassemia Trait" to tr("Thalassemia Trait", "થેલેસેમિયા ટ્રેટ", "थैलेसीमिया ट्रेट"),
        "Pharmacogenomic Sensitivity" to tr("Drug Sensitivity", "દવાની સંવેદનશીલતા", "दवा संवेदनशीलता")
    )

    val goalPresets = listOf(
        "Cardiac & BP Care" to tr("Cardiac & BP", "હૃદય અને BP", "हृदय और BP"),
        "Glycemic Control" to tr("Sugar Control", "સુગર નિયંત્રણ", "शुगर नियंत्रण"),
        "Preventive Longevity" to tr("Preventive Wellness", "નિવારક આરોગ્ય", "निवारक स्वास्थ्य"),
        "Thyroid & Immunity" to tr("Immunity & Thyroid", "રોગપ્રતિકારક શક્તિ", "इम्युनिटी और थायरॉइड")
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Biotech,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            "Personalized Treatment Plans",
                            "વ્યક્તિગત સારવાર યોજના (Precision AI)",
                            "व्यक्तिगत उपचार योजना (Precision AI)"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Tailored care from genetics, lifestyle & lab test results",
                            "દર્દીના જીનેટિક્સ, જીવનશૈલી અને ટેસ્ટ રિઝલ્ટ મુજબ શ્રેષ્ઠ સારવાર વિકલ્પો",
                            "मरीज के जेनेटिक्स, जीवनशैली और लैब रिपोर्ट के अनुसार व्यक्तिगत उपचार विकल्प"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Patient Profile Context Summary Pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            val profText = if (userProfile != null) {
                "${userProfile!!.fullName} • ${userProfile!!.age}y • ${userProfile!!.bloodGroup} • ${tr("Allergies", "એલર્જી", "एलर्जी")}: ${userProfile!!.allergies}"
            } else {
                tr("Complete Profile for richer allergy cross-checks", "વધુ સચોટ પ્લાન માટે પ્રોફાઇલ પૂર્ણ કરો", "बेहतर योजना के लिए प्रोफ़ाइल भरें")
            }
            Text(
                text = profText,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Inputs Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tr("Health Goal & Focus", "સારવારનું લક્ષ્ય", "उपचार का लक्ष्य"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    goalPresets.forEach { (key, label) ->
                        FilterChip(
                            selected = targetGoal == key,
                            onClick = { targetGoal = key },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = targetGoal,
                    onValueChange = { targetGoal = it },
                    label = { Text(tr("Condition / Treatment Goal", "બિમારી અથવા લક્ષ્ય", "बीमारी या लक्ष्य")) },
                    placeholder = { Text(tr("e.g., Hypertension & Cholesterol Care", "દા.ત., બીપી અને કોલેસ્ટ્રોલ નિયંત્રણ", "जैसे: बीपी और कोलेस्ट्रॉल नियंत्रण")) },
                    modifier = Modifier.fillMaxWidth().testTag("treatment_goal_input")
                )

                Text(
                    text = tr("Genetic & Hereditary Markers", "જીનેટિક અને વારસાગત માહિતી", "जेनेटिक और आनुवंशिक जानकारी"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    geneticPresets.forEach { (marker, label) ->
                        val selected = geneticsInput.contains(marker)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                geneticsInput = if (selected) {
                                    geneticsInput.replace(marker, "").trim(' ', ',')
                                } else if (geneticsInput.isBlank()) marker else "$geneticsInput, $marker"
                            },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = geneticsInput,
                    onValueChange = { geneticsInput = it },
                    label = { Text(tr("Genetics / Family History", "જીનેટિક્સ / પારિવારિક ઇતિહાસ", "जेनेटिक्स / पारिवारिक इतिहास")) },
                    placeholder = { Text(tr("e.g., CYP2C19, ApoE4, Father Type-2 Diabetes", "દા.ત., પારિવારિક ડાયાબિટીસ, BP", "जैसे: पारिवारिक मधुमेह, BP")) },
                    modifier = Modifier.fillMaxWidth().testTag("treatment_genetics_input")
                )

                OutlinedTextField(
                    value = lifestyleInput,
                    onValueChange = { lifestyleInput = it },
                    label = { Text(tr("Lifestyle (Diet, Sleep, Exercise)", "જીવનશૈલી (ખોરાક, ઊંઘ, કસરત)", "जीवनशैली (खानपान, नींद, व्यायाम)")) },
                    placeholder = { Text(tr("e.g., Vegetarian, 6 hrs sleep, desk job, mild stress", "દા.ત., શાકાહારી, ૬ કલાક ઊંઘ, બેઠાડુ કામ", "जैसे: शाकाहारी, 6 घंटे नींद, तनाव")) },
                    modifier = Modifier.fillMaxWidth().testTag("treatment_lifestyle_input")
                )

                OutlinedTextField(
                    value = labInput,
                    onValueChange = { labInput = it },
                    label = { Text(tr("Lab & Test Results (Auto-linked + Custom)", "લેબ અને ટેસ્ટ રિઝલ્ટ", "लैब और टेस्ट परिणाम")) },
                    placeholder = { Text(tr("e.g., HbA1c 6.8%, LDL 145, Vitamin D 18", "દા.ત., HbA1c 6.8%, LDL 145, Vit D 18", "जैसे: HbA1c 6.8%, LDL 145, Vit D 18")) },
                    modifier = Modifier.fillMaxWidth().testTag("treatment_labs_input")
                )

                Button(
                    onClick = {
                        viewModel.generatePersonalizedTreatmentPlan(
                            targetGoal = targetGoal,
                            geneticMarkers = geneticsInput,
                            lifestyleSummary = lifestyleInput,
                            labResultsSummary = labInput
                        )
                    },
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("generate_treatment_plan_btn")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Synthesizing Personalized Plan...", "વ્યક્તિગત યોજના તૈયાર થઈ રહી છે...", "व्यक्तिगत योजना बन रही है..."))
                    } else {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Generate Personalized Treatment Plan", "વ્યક્તિગત સારવાર યોજના બનાવો", "व्यक्तिगत उपचार योजना बनाएं"))
                    }
                }
            }
        }

        // Active Generated Plan
        if (!activePlan.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SafeMint.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth().testTag("treatment_plan_result_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr("Personalized Care Roadmap", "તમારી વ્યક્તિગત સારવાર યોજના", "आपकी व्यक्तिगत उपचार योजना"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SafeMint
                        )
                        IconButton(onClick = { viewModel.speakText(activePlan!!, null) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = tr("Listen", "સાંભળો", "सुनें"),
                                tint = SafeMint
                            )
                        }
                    }

                    Text(
                        text = activePlan!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedButton(
                        onClick = {
                            val summaryMsg = trLang(
                                selectedLanguage,
                                "Please review my Personalized Treatment Plan for ${targetGoal.ifBlank { "Wellness" }} and answer follow-up questions.",
                                "મારી વ્યક્તિગત સારવાર યોજના (${targetGoal.ifBlank { "આરોગ્ય" }}) વિશે વધુ માર્ગદર્શન આપો.",
                                "मेरी व्यक्तिगत उपचार योजना (${targetGoal.ifBlank { "स्वास्थ्य" }}) पर आगे मार्गदर्शन दें।"
                            )
                            viewModel.sendMessage(summaryMsg)
                            viewModel.setScreen(AppScreen.TRIAGE_CHAT)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Discuss Plan in AI Triage Chat", "AI ચેટમાં ડૉક્ટર-મિત્ર સાથે ચર્ચા કરો", "AI चैट में इस योजना पर चर्चा करें"))
                    }
                }
            }
        }

        // Saved Treatment Plans History
        if (savedPlans.isNotEmpty()) {
            Text(
                text = tr("Saved Treatment Plans (${savedPlans.size})", "સાચવેલી સારવાર યોજનાઓ (${savedPlans.size})", "सहेजी गई उपचार योजनाएं (${savedPlans.size})"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            val dateFmt = remember { SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()) }
            savedPlans.take(5).forEach { plan ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plan.targetGoal, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${dateFmt.format(Date(plan.timestamp))} • ${plan.geneticMarkers}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.deleteTreatmentPlan(plan) }) {
                                Icon(Icons.Default.Delete, contentDescription = tr("Delete", "કાઢી નાખો", "हटाएं"), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = plan.planContent.take(260) + if (plan.planContent.length > 260) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
