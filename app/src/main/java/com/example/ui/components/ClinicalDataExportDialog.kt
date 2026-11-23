package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.utils.ExportFormat

@Composable
fun ClinicalDataExportDialog(
    adherenceCount: Int,
    symptomCount: Int,
    noteCount: Int,
    initialFormat: ExportFormat = ExportFormat.CSV,
    generatePreview: (ExportFormat) -> String,
    onSaveFile: (ExportFormat) -> String?,
    onShareFile: (ExportFormat) -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage = LocalAppLanguage.current
    var selectedFormat by remember { mutableStateOf(initialFormat) }
    var isCopied by remember { mutableStateOf(false) }

    val previewContent = remember(selectedFormat, adherenceCount, symptomCount, noteCount) {
        generatePreview(selectedFormat)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (selectedFormat == ExportFormat.CSV) {
                                    Icons.Default.TableChart
                                } else {
                                    Icons.Default.Code
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tr(
                                "Export Health Data",
                                "આરોગ્ય ડેટા એક્સપોર્ટ",
                                "स्वास्थ्य डेटा एक्सपोर्ट"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                "CSV & JSON for Healthcare Provider",
                                "ડૉક્ટર માટે CSV અને JSON ફાઇલ",
                                "डॉक्टर के लिए CSV और JSON फ़ाइल"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("export_dialog_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = tr("Close", "બંધ કરો", "बंद करें")
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Summary counts banner
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExportMetricBadge(
                            value = adherenceCount.toString(),
                            label = tr("Adherence", "દવા લોગ", "दवा लॉग")
                        )
                        ExportMetricBadge(
                            value = symptomCount.toString(),
                            label = tr("Symptoms", "લક્ષણો", "लक्षण")
                        )
                        ExportMetricBadge(
                            value = noteCount.toString(),
                            label = tr("Notes", "નોટ્સ", "नोट्स")
                        )
                    }
                }

                // Format switcher chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFormat == ExportFormat.CSV,
                        onClick = {
                            selectedFormat = ExportFormat.CSV
                            isCopied = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tr("CSV Spreadsheet", "CSV ફાઇલ", "CSV स्प्रेडशीट"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("export_format_csv_chip")
                    )
                    FilterChip(
                        selected = selectedFormat == ExportFormat.JSON,
                        onClick = {
                            selectedFormat = ExportFormat.JSON
                            isCopied = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tr("JSON Structured", "JSON ડેટા", "JSON डेटा"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("export_format_json_chip")
                    )
                }

                Text(
                    text = tr(
                        "Preview (${selectedFormat.label}):",
                        "પૂર્વાવલોકન (${selectedFormat.label}):",
                        "पूर्वावलोकन (${selectedFormat.label}):"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 210.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = previewContent.take(6000),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                                .testTag("export_raw_preview_text")
                        )
                    }
                }

                // Copy raw data button
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(
                            "Health Care AI ${selectedFormat.label}",
                            previewContent
                        )
                        clipboard.setPrimaryClip(clip)
                        isCopied = true
                        Toast.makeText(
                            context,
                            trLang(
                                currentLanguage,
                                "${selectedFormat.label} data copied to clipboard",
                                "${selectedFormat.label} ડેટા ક્લિપબોર્ડમાં કોપી થયો",
                                "${selectedFormat.label} डेटा क्लिपबोर्ड पर कॉपी हो गया"
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("export_copy_raw_btn")
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCopied) {
                            tr("Copied!", "કોપી થયું!", "कॉपी हो गया!")
                        } else {
                            tr(
                                "Copy ${selectedFormat.label}",
                                "${selectedFormat.label} કોપી કરો",
                                "${selectedFormat.label} कॉपी करें"
                            )
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val savedPath = onSaveFile(selectedFormat)
                        val msg = if (savedPath != null) {
                            trLang(
                                currentLanguage,
                                "Saved ${selectedFormat.label} to $savedPath",
                                "${selectedFormat.label} ફાઇલ સેવ થઈ: $savedPath",
                                "${selectedFormat.label} फ़ाइल सहेजी गई: $savedPath"
                            )
                        } else {
                            trLang(
                                currentLanguage,
                                "Failed to save ${selectedFormat.label} file",
                                "${selectedFormat.label} ફાઇલ સેવ કરવામાં નિષ્ફળ",
                                "${selectedFormat.label} फ़ाइल सहेजने में विफल"
                            )
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("export_save_file_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr(
                            "Save ${selectedFormat.label}",
                            "${selectedFormat.label} સાચવો",
                            "${selectedFormat.label} सहेजें"
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val launched = onShareFile(selectedFormat)
                        if (!launched) {
                            Toast.makeText(
                                context,
                                trLang(
                                    currentLanguage,
                                    "Unable to open share sheet",
                                    "શેર વિન્ડો ખોલવામાં નિષ્ફળ",
                                    "शेयर शीट खोलने में विफल"
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("export_share_file_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Share", "શેર કરો", "शेयर करें"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun ExportMetricBadge(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
