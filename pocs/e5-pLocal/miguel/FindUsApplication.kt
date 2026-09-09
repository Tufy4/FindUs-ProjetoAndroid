package com.example.findus

import android.app.Application
import com.example.findus.di.AppContainer

class FindUsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
