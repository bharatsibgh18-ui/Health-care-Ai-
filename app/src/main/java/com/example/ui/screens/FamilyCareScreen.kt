package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FamilyMemberEntity
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyCareScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val familyMembers by viewModel.familyMembers.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddFamilyMemberDialog(
            language = selectedLanguage,
            onDismiss = { showAddDialog = false },
            onSave = { name, rel, age, bg, phone, cond ->
                viewModel.addFamilyMember(name, rel, age, bg, phone, cond)
                showAddDialog = false
                Toast.makeText(
                    context,
                    trLang(
                        selectedLanguage,
                        "Family Member Profile Saved",
                        "પરિવારના સભ્યની પ્રોફાઇલ સેવ થઈ ગઈ",
                        "परिवार के सदस्य की प्रोफ़ाइल सहेजी गई"
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FamilyRestroom,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = tr(
                                    "Family & Caregiver Support",
                                    "પરિવાર અને કેરગીવર કેર",
                                    "परिवार और केयरगिवर सहायता"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Elder Monitoring & Auto Alerts",
                                    "વડીલોની દેખરેખ અને ઓટો એલર્ટ",
                                    "बुज़ुर्गों की देखभाल और ऑटो अलर्ट"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.TRIAGE_CHAT) },
                        modifier = Modifier.testTag("family_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "પાછા જાઓ", "वापस जाएं")
                        )
                    }
                },
                actions = {
                    com.example.ui.components.LanguageSelector(viewModel = viewModel, showRefreshButton = false)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_family_member_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Add Member", "સભ્ય ઉમેરો", "सदस्य जोड़ें"), fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Caregiver Alert Explainer Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(
                                "Automatic Elder Caregiver Alerts",
                                "વૃદ્ધો માટે ઓટોમેટિક કેરગીવર એલર્ટ",
                                "बुज़ुर्गों के लिए स्वचालित केयरगिवर अलर्ट"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = tr(
                                "Dispatches automatic caregiver SMS alert if medication is missed by elders or children.",
                                "જો વડીલો સમયસર દવા ન લે, તો કેરગીવરના ફોન પર તરત SMS એલર્ટ મોકલી શકાય છે.",
                                "यदि बुज़ुर्ग या बच्चे समय पर दवा न लें, तो केयरगिवर को तुरंत SMS अलर्ट भेजा जा सकता है।"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (familyMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FamilyRestroom, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = tr(
                                "No Family Profiles Added Yet",
                                "કોઈ કુટુંબ સભ્ય ઉમેરેલ નથી",
                                "अभी तक कोई परिवार प्रोफ़ाइल नहीं जोड़ी गई"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tr(
                                "Add parents, children or elders to manage health logs & reminders separately.",
                                "માતા-પિતા કે બાળકોનું પ્રોફાઇલ ઉમેરી તેમના સ્વાસ્થ્ય અને દવાઓનું સંચાલન કરો.",
                                "माता-पिता, बच्चों या बुज़ुर्गों की प्रोफ़ाइल जोड़ें और उनके स्वास्थ्य व दवाइयों का प्रबंधन करें।"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(familyMembers, key = { it.id }) { member ->
                        FamilyMemberCard(
                            member = member,
                            onDelete = { viewModel.deleteFamilyMember(member) },
                            onTestCaregiverAlert = {
                                if (member.phone.isNotBlank()) {
                                    viewModel.triggerCaregiverEmergencyAlert(
                                        context = context,
                                        elderName = member.name,
                                        medName = trLang(
                                            selectedLanguage,
                                            "Morning BP Medicine",
                                            "સવારની BP ની દવા",
                                            "सुबह की BP की दवा"
                                        ),
                                        caregiverPhone = member.phone
                                    )
                                    Toast.makeText(
                                        context,
                                        trLang(
                                            selectedLanguage,
                                            "Caregiver alert dispatched to ${member.phone}",
                                            "${member.phone} પર કેરગીવર એલર્ટ મોકલવામાં આવ્યો",
                                            "${member.phone} पर केयरगिवर अलर्ट भेजा गया"
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        trLang(
                                            selectedLanguage,
                                            "Please set a caregiver phone number first",
                                            "કૃપા કરીને પહેલા કેરગીવરનો ફોન નંબર ઉમેરો",
                                            "कृपया पहले केयरगिवर का फ़ोन नंबर जोड़ें"
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyMemberCard(
    member: FamilyMemberEntity,
    onDelete: () -> Unit,
    onTestCaregiverAlert: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().testTag("family_member_card_${member.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = member.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = member.relationship,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = tr(
                            "Age: ${member.age} • Blood: ${member.bloodGroup} • ${member.chronicConditions}",
                            "ઉંમર: ${member.age} • બ્લડ: ${member.bloodGroup} • ${member.chronicConditions}",
                            "उम्र: ${member.age} • ब्लड: ${member.bloodGroup} • ${member.chronicConditions}"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = tr("Delete", "કાઢી નાખો", "हटाएं"),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTestCaregiverAlert,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        tr(
                            "Test Caregiver Alert",
                            "કેરગીવર એલર્ટ ટેસ્ટ કરો",
                            "केयरगिवर अलर्ट टेस्ट करें"
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AddFamilyMemberDialog(
    language: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var relationship by remember {
        mutableStateOf(trLang(language, "Mother", "માતા", "माता"))
    }
    var ageText by remember { mutableStateOf("58") }
    var bloodGroup by remember { mutableStateOf("B+") }
    var phone by remember { mutableStateOf("") }
    var conditions by remember {
        mutableStateOf(
            trLang(
                language,
                "Blood Pressure / Diabetes",
                "બ્લડ પ્રેશર / ડાયાબિટીસ",
                "ब्लड प्रेशर / डायबिटीज़"
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                trLang(
                    language,
                    "Add Family Member Profile",
                    "કુટુંબ સભ્યની પ્રોફાઇલ ઉમેરો",
                    "परिवार के सदस्य की प्रोफ़ाइल जोड़ें"
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(trLang(language, "Full Name", "પૂરું નામ", "पूरा नाम")) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = relationship,
                        onValueChange = { relationship = it },
                        label = { Text(trLang(language, "Relationship", "સંબંધ", "संबंध")) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it },
                        label = { Text(trLang(language, "Age", "ઉંમર", "उम्र")) },
                        modifier = Modifier.weight(0.7f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = { bloodGroup = it },
                        label = { Text(trLang(language, "Blood Group", "બ્લડ ગ્રુપ", "ब्लड ग्रुप")) },
                        modifier = Modifier.weight(0.7f)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(trLang(language, "Caregiver Phone", "કેરગીવર મોબાઇલ", "केयरगिवर फ़ोन")) },
                        modifier = Modifier.weight(1.3f)
                    )
                }
                OutlinedTextField(
                    value = conditions,
                    onValueChange = { conditions = it },
                    label = { Text(trLang(language, "Chronic Conditions", "લાંબા ગાળાની બીમારી", "पुरानी बीमारियां")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    val age = ageText.toIntOrNull() ?: 30
                    onSave(name, relationship, age, bloodGroup, phone, conditions)
                }
            }) {
                Text(trLang(language, "Save", "સેવ કરો", "सहेजें"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(trLang(language, "Cancel", "રદ કરો", "रद्द करें"))
            }
        }
    )
}
