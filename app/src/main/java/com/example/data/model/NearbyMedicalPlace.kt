package com.example.data.model

data class NearbyMedicalPlace(
    val id: String,
    val name: String,
    val type: String, // "Hospital", "Clinic", "Pharmacy", "Trauma Center"
    val address: String,
    val distanceKm: Double,
    val phoneNumber: String,
    val rating: Double = 4.8,
    val openHours: String = "24 Hours Open",
    val latitude: Double,
    val longitude: Double,
    val isEmergencyRoom: Boolean = true,
    val availableBedsEstimated: Int = 14
)
