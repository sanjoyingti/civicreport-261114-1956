package com.arin.civicreport.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Database row for a reportable authority (PWD, CM office, PMO, etc.). */
@Entity(tableName = "authorities")
data class Authority(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val level: String,                  // "Local" / "State" / "Central"
    val activity_tags: String,          // "pothole,waterlogging"
    val applicable_states: String,      // "Maharashtra"
    val applicable_cities: String,      // "mumbai"
    val order: Int
)
