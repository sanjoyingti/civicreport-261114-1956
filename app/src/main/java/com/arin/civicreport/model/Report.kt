package com.arin.civicreport.model

enum class IncidentStatus { Draft, Pending, Sent, Failed }

data class Report(
    val id: Long = 0,
    val incidentTypes: List<String>,
    val mediaUris: List<String>,
    val mediaType: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String,
    val district: String,
    val state: String,
    val pincode: String,
    val description: String,
    val status: IncidentStatus,
    val sentAuthorities: List<String>,
    val lastUpdated: Long,
    val sendMethod: String
)
