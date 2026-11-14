package com.arin.civicreport.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Database row for a civic incident report. JSON fields are stored as comma strings so Room never fails on missing types. */
@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val incident_types: String,          // "pothole,waterlogging"
    val media_uris: String,              // content:// or file:// URIs
    val media_type: String,              // "photo" or "video"
    val timestamp: Long,                 // epoch ms when created
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String,
    val district: String,
    val state: String,
    val pincode: String,
    val description: String,
    val status: String,                  // "Draft" / "Pending" / "Sent" / "Failed"
    val sent_authorities: String,        // authority ids joined
    val last_updated: Long,
    val send_method: String              // "email" / "sms" / "both"
)
