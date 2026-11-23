package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.DeveloperLogEntity
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeveloperLogsScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val persistedLogs by viewModel.persistedDeveloperLogs.collectAsStateWithLifecycle()
    val liveLogs by viewModel.liveDeveloperLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedLevel by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    // Combine persisted Room logs with any buffered live logs (deduplicated by timestamp+tag+message)
    val displayLogs = remember(persistedLogs, liveLogs, selectedLevel, searchQuery) {
        val fromRoom = persistedLogs.ifEmpty {
            liveLogs.map {
                DeveloperLogEntity(
                    id = it.id,
                    level = it.level,
                    tag = it.tag,
                    message = it.message,
                    timestamp = it.timestamp
                )
            }
        }
        fromRoom.filter { log ->
            val levelMatches = selectedLevel == "ALL" || log.level.equals(selectedLevel, ignoreCase = true)
            val queryMatches = searchQuery.isBlank() ||
                log.tag.contains(searchQuery, ignoreCase = true) ||
                log.message.contains(searchQuery, ignoreCase = true)
            levelMatches && queryMatches
        }
    }

    val timeFmt = remember { SimpleDateFormat("dd MMM HH:mm:ss.SSS", Locale.US) }

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
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = tr("Developer Logs Console", "ડેવલપર લોગ્સ કન્સોલ", "डेवलपर लॉग्स कंसोल"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${displayLogs.size} events • Room + Logcat",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.TRIAGE_CHAT) },
                        modifier = Modifier.testTag("dev_logs_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "પાછા જાઓ", "वापस जाएं")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.DEVELOPER_CONSOLE) },
                        modifier = Modifier.testTag("dev_logs_open_console_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Open Developer Control & Upgrades",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            val exportText = displayLogs.joinToString("\n") { entry ->
                                "[${timeFmt.format(Date(entry.timestamp))}] ${entry.level}/${entry.tag}: ${entry.message}"
                            }
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("HealthCareAI Developer Logs", exportText))
                            Toast.makeText(context, "Copied ${displayLogs.size} logs", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("dev_log_copy_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Logs")
                    }
                    IconButton(
                        onClick = {
                            viewModel.clearDeveloperLogs()
                            Toast.makeText(context, "Developer logs cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("dev_log_clear_btn")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs")
                    }
                    IconButton(
                        onClick = { viewModel.logoutAccount() },
                        modifier = Modifier.testTag("dev_log_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out Developer",
                            tint = EmergencyRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter by tag or message...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dev_log_search_input")
                )
                Button(
                    onClick = {
                        viewModel.emitDeveloperLog(
                            level = "INFO",
                            tag = "DevDiagnostics",
                            message = "Manual diagnostic ping • Room logs=${persistedLogs.size}, activeMeds=${viewModel.medicines.value.size}"
                        )
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("dev_log_test_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ping", fontWeight = FontWeight.Bold)
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "DEBUG", "INFO", "WARN", "ERROR").forEach { level ->
                    FilterChip(
                        selected = selectedLevel == level,
                        onClick = { selectedLevel = level },
                        label = { Text(level, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("dev_log_filter_$level")
                    )
                }
            }

            if (displayLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No developer logs matching filter",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            viewModel.emitDeveloperLog("DEBUG", "DevConsole", "Initialized live logging check")
                        }) {
                            Text("Generate Sample Log")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayLogs, key = { "${it.id}_${it.timestamp}" }) { entry ->
                        val levelColor = when (entry.level.uppercase()) {
                            "ERROR" -> EmergencyRed
                            "WARN" -> Color(0xFFE65100)
                            "INFO" -> PrimaryTeal
                            else -> SafeMint
                        }
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dev_log_item_${entry.id}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = levelColor.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = entry.level,
                                                color = levelColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = entry.tag,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = timeFmt.format(Date(entry.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = entry.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
