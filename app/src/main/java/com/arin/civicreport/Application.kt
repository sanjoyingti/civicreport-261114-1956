package com.arin.civicreport

import android.content.Context
import org.osmdroid.config.Configuration

class Application : android.app.Application() {

    override fun onCreate() {
        super.onCreate()
        val config = Configuration.getInstance()
        config.userAgentValue = packageName
        config.load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }
}
