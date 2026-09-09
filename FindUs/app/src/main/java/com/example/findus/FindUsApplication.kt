package com.example.findus

import android.app.Application
import com.example.findus.di.AppContainer
import org.osmdroid.config.Configuration

class FindUsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(this@FindUsApplication, getSharedPreferences("osmdroid_prefs", MODE_PRIVATE))
        }
        container = AppContainer(this)
    }
}
