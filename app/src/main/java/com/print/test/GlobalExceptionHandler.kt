package com.print.test

import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.lang.Thread.UncaughtExceptionHandler
import kotlin.system.exitProcess

class GlobalExceptionHandler : UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Handle the exception here
        // You can log the exception, show an error message, or perform any desired actions

        // For example, you can log the exception
        throwable.printStackTrace()
        FirebaseCrashlytics.getInstance()
            .recordException(Exception("From [UNCAUGHTEXCEPTION]    ::    ${throwable.stackTraceToString()}"))

        // Terminate the application (optional)
        exitProcess(1)
    }
}