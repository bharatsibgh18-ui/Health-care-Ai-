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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.viewmodel.MediAssistViewModel

@Composable
fun DrugDiscoveryPane(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val isScreening by viewModel.isScreeningDrug.collectAsState()
    val metrics by viewModel.activeDrugMetrics.collectAsState()
    val activeReport by viewModel.activeDrugReport.collectAsState()
    val savedStudies by viewModel.drugStudies.collectAsState()

    var targetDisease by remember { mutableStateOf("Viral Protease / Dengue NS5") }
    var moleculeInput by remember { mutableStateOf("C16H18N2O4S") }
    var priority by remember { mutableStateOf("Safety & Oral Bioavailability") }

    val presets = listOf(
        Triple("Aspirin Analog", "C9H8O4", "Anti-inflammatory COX Inhibitor"),
        Triple("Penicillin Core", "C16H18N2O4S", "Beta-Lactam Antibiotic Target"),
        Triple("Antiviral Lead", "CC(=O)Nc1ccc(O)cc1", "Viral RNA Polymerase")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Banner
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
                        Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            "AI Drug Discovery & Clinical Trials",
                            "દવાની નવી શોધ (Drug Discovery & Trials)",
                            "AI ड्रग डिस्कवरी और क्लिनिकल ट्रायल"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Rapidly screen chemical formulas, Lipinski Rule-of-5, ADMET safety & clinical trial readiness in months",
                            "લાખો કેમિકલ ફોર્મ્યુલાનું ઝડપી એનાલિસિસ કરી નવી દવા શોધવાની પ્રક્રિયા વર્ષોને બદલે મહિનાઓમાં પૂરી કરો",
                            "रासायनिक फ़ॉर्मूला, ADMET सुरक्षा और क्लिनिकल ट्रायल की तैयारी का तेज़ AI विश्लेषण"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Preset Compounds
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (label, formula, target) ->
                SuggestionChip(
                    onClick = {
                        moleculeInput = formula
                        targetDisease = target
                    },
                    label = { Text("$label ($formula)") }
                )
            }
        }

        // Molecule Input Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = targetDisease,
                    onValueChange = { targetDisease = it },
                    label = { Text(tr("Target Disease / Protein", "લક્ષ્ય રોગ / પ્રોટીન", "लक्ष्य रोग / प्रोटीन")) },
                    modifier = Modifier.fillMaxWidth().testTag("drug_target_input")
                )

                OutlinedTextField(
                    value = moleculeInput,
                    onValueChange = { moleculeInput = it },
                    label = { Text(tr("Chemical Formula or SMILES", "કેમિકલ ફોર્મ્યુલા અથવા SMILES", "रासायनिक फ़ॉर्मूला या SMILES")) },
                    modifier = Modifier.fillMaxWidth().testTag("drug_formula_input")
                )

                Text(tr("Optimization Priority", "પ્રાથમિકતા", "प्राथमिकता"), style = MaterialTheme.typography.labelLarge)
                val goals = listOf("Safety & Oral Bioavailability", "Target Potency", "Low Toxicity (ADMET)")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    goals.forEach { goal ->
                        FilterChip(
                            selected = priority == goal,
                            onClick = { priority = goal },
                            label = { Text(goal) }
                        )
                    }
                }

                Button(
                    onClick = { viewModel.runDrugDiscoveryStudy(targetDisease, moleculeInput, priority) },
                    enabled = targetDisease.isNotBlank() && moleculeInput.isNotBlank() && !isScreening,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("drug_screen_btn")
                ) {
                    if (isScreening) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(tr("Screen Compound & ADMET", "મોલેક્યુલ સ્ક્રીનિંગ કરો", "मॉलिक्यूल स्क्रीनिंग करें"))
                }
            }
        }

        // Lipinski & ADMET Metrics Card
        metrics?.let { m ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SafeMint.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth().testTag("drug_metrics_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tr(
                            "Drug-Likeness Score: ${m.drugLikenessScore}% • ${m.bioavailabilityTier}",
                            "ડ્રગ-લાઇકનેસ સ્કોર: ${m.drugLikenessScore}% • ${m.bioavailabilityTier}",
                            "ड्रग-लाइकनेस स्कोर: ${m.drugLikenessScore}% • ${m.bioavailabilityTier}"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SafeMint
                    )
                    LinearProgressIndicator(
                        progress = { m.drugLikenessScore / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("MW: ${m.estimatedMolecularWeight} g/mol", style = MaterialTheme.typography.labelMedium)
                        Text("Atoms: ${m.heavyAtomEstimate}", style = MaterialTheme.typography.labelMedium)
                        Text("HBD/HBA: ${m.hBondDonors}/${m.hBondAcceptors}", style = MaterialTheme.typography.labelMedium)
                        Text("Violations: ${m.lipinskiViolations}", style = MaterialTheme.typography.labelMedium)
                    }
                    activeReport?.let { rep ->
                        Text(rep, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Saved Studies Archive
        if (savedStudies.isNotEmpty()) {
            Text(
                text = tr("Screened Drug Candidates (${savedStudies.size})", "સાચવેલા દવા અભ્યાસો (${savedStudies.size})", "सहेजे गए ड्रग अध्ययन (${savedStudies.size})"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            savedStudies.take(6).forEach { study ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${study.targetDisease} • ${study.moleculeInput}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(tr("Drug-Likeness: ${study.drugLikenessScore}%", "સ્કોર: ${study.drugLikenessScore}%", "स्कोर: ${study.drugLikenessScore}%"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.deleteDrugStudy(study) }) {
                            Icon(Icons.Default.Delete, contentDescription = tr("Delete", "કાઢો", "हटाएं"), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
