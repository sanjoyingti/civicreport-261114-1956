package com.arin.civicreport.ui.util

import android.content.Context
import android.content.SharedPreferences
import com.arin.civicreport.model.IncidentType

class Prefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("civicreport_prefs", Context.MODE_PRIVATE)

    var enableLocationServices: Boolean
        get() = prefs.getBoolean("location_enabled", true)
        set(v) = prefs.edit().putBoolean("location_enabled", v).apply()

    var defaultSendMethod: String
        get() = prefs.getString("default_send_method", "both") ?: "both"
        set(v) = prefs.edit().putString("default_send_method", v).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(v) = prefs.edit().putBoolean("notifications_enabled", v).apply()

    fun incidentTypeToTag(slug: String): String = slug

    fun getIncidentTitles(slugs: List<String>): List<String> =
        slugs.map { IncidentType.all().find { t -> t.slug == it }?.title ?: it }
}
