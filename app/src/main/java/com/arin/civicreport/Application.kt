package com.arin.civicreport

import android.app.Application
import com.arin.civicreport.data.CivicDatabase
import com.arin.civicreport.data.Repository
import kotlinx.coroutines.runBlocking

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        runBlocking {
            val database = CivicDatabase.getInstance(this@Application)
            val repo = Repository(database)
            repo.seedAuthorities()
        }
    }
}
