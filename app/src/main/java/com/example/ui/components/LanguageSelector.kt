package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.MediAssistViewModel

@Composable
fun LanguageSelector(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier,
    showRefreshButton: Boolean = true
) {
    val context = LocalContext.current
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                modifier = Modifier.testTag("global_language_selector")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Language",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (selectedLanguage) {
                            "Gujarati" -> "ગુજરાતી"
                            "Hindi" -> "हिन्दी"
                            else -> "English"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text("English", fontWeight = if (selectedLanguage == "English") FontWeight.Bold else FontWeight.Normal)
                    },
                    onClick = {
                        viewModel.setSelectedLanguage("English")
                        expanded = false
                        Toast.makeText(context, "Language changed to English", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("lang_option_english")
                )
                DropdownMenuItem(
                    text = {
                        Text("ગુજરાતી (Gujarati)", fontWeight = if (selectedLanguage == "Gujarati") FontWeight.Bold else FontWeight.Normal)
                    },
                    onClick = {
                        viewModel.setSelectedLanguage("Gujarati")
                        expanded = false
                        Toast.makeText(context, "ભાષા બદલાઈને ગુજરાતી થઈ ગઈ", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("lang_option_gujarati")
                )
                DropdownMenuItem(
                    text = {
                        Text("हिन्दी (Hindi)", fontWeight = if (selectedLanguage == "Hindi") FontWeight.Bold else FontWeight.Normal)
                    },
                    onClick = {
                        viewModel.setSelectedLanguage("Hindi")
                        expanded = false
                        Toast.makeText(context, "भाषा बदलकर हिन्दी हो गई", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("lang_option_hindi")
                )
            }
        }

        if (showRefreshButton) {
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                onClick = {
                    viewModel.refreshAppLanguage()
                    val toastMessage = if (selectedLanguage == "Gujarati") {
                        "ભાષા અને એપ્લિકેશન સફળતાપૂર્વક રિફ્રેશ થઈ ગઈ છે!"
                    } else if (selectedLanguage == "Hindi") {
                        "भाषा और ऐप सफलतापूर्वक रीफ्रेश हो गए!"
                    } else {
                        "Language and app refreshed successfully!"
                    }
                    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(34.dp)
                    .testTag("language_refresh_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.btn_refresh_language),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

