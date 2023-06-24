package com.print.test

import android.app.Application

class MyApp:Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler())
    }
}