package com.arin.civicreport

import android.app.Application
import com.arin.civicreport.data.CivicDatabase
import com.arin.civicreport.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class Application : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val repository = Repository(CivicDatabase.getInstance(this))
        scope.launch {
            runCatching {
                val existing = repository.allAuthoritiesOnce()
                if (existing.isEmpty()) repository.seedAuthorities()
            }
        }
    }
}
