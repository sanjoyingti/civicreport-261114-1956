package com.arin.civicreport.model

data class Authority(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val level: String,
    val activityTags: List<String>,
    val applicableStates: List<String>,
    val applicableCities: List<String>,
    val order: Int
)
