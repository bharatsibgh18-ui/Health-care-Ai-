package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.LanguageSelector
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import com.example.data.model.NearbyMedicalPlace
import com.example.utils.NearbyHospitalsService
import com.example.ui.components.dialNumber
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.OnEmergencyRed
import com.example.ui.theme.OnEmergencyRedContainer
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.tr
import com.example.ui.viewmodel.MediAssistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyGuideScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val emergencyContactName = userProfile?.emergencyContactName?.ifBlank { null }
        ?: tr("Personal Contact", "અંગત સંપર્ક", "व्यक्तिगत संपर्क")
    val emergencyContactPhone = userProfile?.emergencyContactPhone?.ifBlank { "112" } ?: "112"

    var selectedTab by remember { mutableIntStateOf(0) }
    val nearbyPlaces by viewModel.nearbyMedicalPlaces.collectAsState()
    val isLocatingHospitals by viewModel.isLocatingHospitals.collectAsState()
    val currentFilter by viewModel.nearbyPlacesFilter.collectAsState()
    val userGpsLocation by viewModel.userCurrentGpsLocation.collectAsState()

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && nearbyPlaces.isEmpty()) {
            viewModel.locateNearbyHospitals(context)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (selectedTab == 0) EmergencyRedContainer else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (selectedTab == 0) Icons.Default.Warning else Icons.Default.LocalHospital,
                                        contentDescription = null,
                                        tint = if (selectedTab == 0) EmergencyRed else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (selectedTab == 0) stringResource(R.string.emergency_protocol_title) else stringResource(R.string.tab_nearby_hospitals),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        if (selectedTab == 1) {
                            IconButton(
                                onClick = { viewModel.locateNearbyHospitals(context) },
                                modifier = Modifier.testTag("refresh_hospitals_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.nearby_gps_refresh),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        LanguageSelector(
                            viewModel = viewModel,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        IconButton(
                            onClick = { viewModel.toggleThemeMode() },
                            modifier = Modifier.testTag("emergency_theme_toggle_button")
                        ) {
                            val (themeIcon, themeName) = when (themeMode) {
                                ThemeMode.DARK -> Icons.Default.DarkMode to stringResource(R.string.theme_dark)
                                ThemeMode.LIGHT -> Icons.Default.LightMode to stringResource(R.string.theme_light)
                                ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto to stringResource(R.string.theme_system)
                            }
                            Icon(
                                imageVector = themeIcon,
                                contentDescription = themeName,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Emergency,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 0) EmergencyRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.tab_emergency_sos),
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_sos_first_aid")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.tab_nearby_hospitals),
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_nearby_hospitals")
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // High Alert Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmergencyRedContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emergency_protocol_card")
                ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = null,
                            tint = EmergencyRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.emergency_dispatch_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmergencyRed
                        )
                    }
                    Text(
                        text = stringResource(R.string.emergency_dispatch_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnEmergencyRedContainer,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { dialNumber(context, "112") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dial_112_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmergencyRed,
                                contentColor = OnEmergencyRed
                            )
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.emergency_call_112), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { dialNumber(context, "108") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmergencyRed,
                                contentColor = OnEmergencyRed
                            )
                        ) {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.emergency_call_108), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live GPS SOS SMS Button
                    var isSendingSos by remember { mutableStateOf(false) }
                    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                    ) { _ ->
                        isSendingSos = true
                        viewModel.triggerEmergencySosWithLocation(context) { _, msg ->
                            isSendingSos = false
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                        }
                    }

                    Button(
                        onClick = {
                            val fineLocGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                            val smsGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.SEND_SMS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                            if (fineLocGranted && smsGranted) {
                                isSendingSos = true
                                viewModel.triggerEmergencySosWithLocation(context) { _, msg ->
                                    isSendingSos = false
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                        android.Manifest.permission.SEND_SMS
                                    )
                                )
                            }
                        },
                        enabled = !isSendingSos,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_sos_sms_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmergencyRed,
                            contentColor = OnEmergencyRed
                        )
                    ) {
                        if (isSendingSos) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = OnEmergencyRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.emergency_sending_gps_sos), fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.emergency_send_gps_sos), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { dialNumber(context, emergencyContactPhone) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dial_contact_button")
                    ) {
                        Icon(Icons.Default.ContactPhone, contentDescription = null, tint = EmergencyRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                R.string.emergency_call_contact_format,
                                emergencyContactName,
                                emergencyContactPhone
                            ),
                            color = EmergencyRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Life-Threatening Red Flags List
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.emergency_red_flags_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmergencyRed
                    )
                    Text(
                        text = stringResource(R.string.emergency_red_flags_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    EmergencySignItem(
                        title = stringResource(R.string.emergency_sign_chest_title),
                        description = stringResource(R.string.emergency_sign_chest_desc)
                    )
                    EmergencySignItem(
                        title = stringResource(R.string.emergency_sign_breath_title),
                        description = stringResource(R.string.emergency_sign_breath_desc)
                    )
                    EmergencySignItem(
                        title = stringResource(R.string.emergency_sign_stroke_title),
                        description = stringResource(R.string.emergency_sign_stroke_desc)
                    )
                    EmergencySignItem(
                        title = stringResource(R.string.emergency_sign_headache_title),
                        description = stringResource(R.string.emergency_sign_headache_desc)
                    )
                    EmergencySignItem(
                        title = stringResource(R.string.emergency_sign_bleeding_title),
                        description = stringResource(R.string.emergency_sign_bleeding_desc)
                    )
                    EmergencySignItem(
                        title = stringResource(R.string.emergency_sign_allergy_title),
                        description = stringResource(R.string.emergency_sign_allergy_desc)
                    )
                }
            }

            // FAST Stroke Identification Guide
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.emergency_fast_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FastCriteriaItem(
                        letter = "F",
                        label = stringResource(R.string.emergency_fast_f_title),
                        description = stringResource(R.string.emergency_fast_f_desc)
                    )
                    FastCriteriaItem(
                        letter = "A",
                        label = stringResource(R.string.emergency_fast_a_title),
                        description = stringResource(R.string.emergency_fast_a_desc)
                    )
                    FastCriteriaItem(
                        letter = "S",
                        label = stringResource(R.string.emergency_fast_s_title),
                        description = stringResource(R.string.emergency_fast_s_desc)
                    )
                    FastCriteriaItem(
                        letter = "T",
                        label = stringResource(R.string.emergency_fast_t_title),
                        description = stringResource(R.string.emergency_fast_t_desc)
                    )
                }
            }

            // Safe First-Aid Protocol
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.emergency_first_aid_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.emergency_fa_heart_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.emergency_fa_heart_desc),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.emergency_fa_allergy_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.emergency_fa_allergy_desc),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.emergency_fa_unconscious_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.emergency_fa_unconscious_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    } else {
        NearbyHospitalsContent(
            places = nearbyPlaces,
            isLoading = isLocatingHospitals,
            selectedFilter = currentFilter,
            userLocation = userGpsLocation,
            paddingValues = paddingValues,
            onFilterSelected = { viewModel.setNearbyPlacesFilter(it) },
            onRefreshLocation = { viewModel.locateNearbyHospitals(context) }
        )
    }
}
}

@Composable
fun NearbyHospitalsContent(
    places: List<NearbyMedicalPlace>,
    isLoading: Boolean,
    selectedFilter: String,
    userLocation: Pair<Double, Double>?,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    onFilterSelected: (String) -> Unit,
    onRefreshLocation: () -> Unit
) {
    val context = LocalContext.current
    val filteredPlaces = remember(places, selectedFilter) {
        if (selectedFilter == "All") places
        else places.filter { it.type.equals(selectedFilter, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Radar / GPS Hero Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("nearby_hospitals_hero_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
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
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.nearby_map_header_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val gpsText = if (userLocation != null) {
                                tr(
                                    "📍 Live GPS: %.4f° N, %.4f° E • Live Search".format(userLocation.first, userLocation.second),
                                    "📍 લાઇવ GPS: %.4f° N, %.4f° E • રીયલ-ટાઇમ શોધ".format(userLocation.first, userLocation.second),
                                    "📍 लाइव GPS: %.4f° N, %.4f° E • रीयल-टाइम खोज".format(userLocation.first, userLocation.second)
                                )
                            } else {
                                stringResource(R.string.nearby_gps_detecting)
                            }
                            Text(
                                text = gpsText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.nearby_map_header_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val lat = userLocation?.first ?: 22.3039
                            val lon = userLocation?.second ?: 70.8022
                            NearbyHospitalsService.openGoogleMapsSearch(
                                context,
                                lat,
                                lon,
                                "emergency hospital"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_search_live_maps")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_open_google_maps),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onRefreshLocation,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_refresh_gps")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                "All" to stringResource(R.string.nearby_filter_all),
                "Hospital" to stringResource(R.string.nearby_filter_hospitals),
                "Clinic" to stringResource(R.string.nearby_filter_clinics),
                "Pharmacy" to stringResource(R.string.nearby_filter_pharmacies)
            )

            filters.forEach { (typeKey, label) ->
                FilterChip(
                    selected = selectedFilter == typeKey,
                    onClick = { onFilterSelected(typeKey) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_chip_$typeKey")
                )
            }
        }

        // List of Nearby Centers
        if (filteredPlaces.isEmpty() && !isLoading) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tr(
                            "No medical centers found in this category.",
                            "આ શ્રેણીમાં કોઈ મેડિકલ સેન્ટર મળ્યા નથી.",
                            "इस श्रेणी में कोई मेडिकल सेंटर नहीं मिला।"
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            filteredPlaces.forEach { place ->
                MedicalPlaceCard(place = place, onNavigate = {
                    NearbyHospitalsService.openNavigationInMaps(context, place.latitude, place.longitude, place.name)
                }, onCall = {
                    NearbyHospitalsService.dialHospitalPhone(context, place.phoneNumber)
                })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MedicalPlaceCard(
    place: NearbyMedicalPlace,
    onNavigate: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (place.isEmergencyRoom) EmergencyRedContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("medical_place_card_${place.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "%.1f km".format(place.distanceKm),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row: Open Hours, Rating, Beds, Emergency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (place.isEmergencyRoom) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmergencyRed,
                        modifier = Modifier.testTag("emergency_tag_${place.id}")
                    ) {
                        Text(
                            text = stringResource(R.string.emergency_room_badge),
                            color = OnEmergencyRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                if (place.availableBedsEstimated > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.nearby_beds_available, place.availableBedsEstimated),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "%.1f".format(place.rating),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Navigate in Maps & Direct Emergency Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigate,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("navigate_btn_${place.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_navigate_facility),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onCall,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EmergencyRed
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("call_btn_${place.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = EmergencyRed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_call_facility),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmergencyRed
                    )
                }
            }
        }
    }
}

@Composable
fun EmergencySignItem(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("• ", fontWeight = FontWeight.Bold, color = EmergencyRed, fontSize = 16.sp)
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FastCriteriaItem(letter: String, label: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = letter,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
