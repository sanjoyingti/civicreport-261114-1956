package com.arin.civicreport

import com.arin.civicreport.data.CivicDatabase
import com.arin.civicreport.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class Application : android.app.Application() {

    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(CivicDatabase.getInstance(this))

        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        CoroutineScope(Dispatchers.IO).launch {
            runCatching { repository.seedAuthorities() }
        }
    }
}
