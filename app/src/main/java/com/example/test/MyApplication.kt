package com.example.test

import android.app.Application
import android.view.Surface
import org.acra.ACRA
import org.acra.ErrorReporter
import org.acra.ReportingInteractionMode
import org.acra.annotation.ReportsCrashes

@ReportsCrashes(
    formKey = "",
    resToastText = R.string.app_name,
    mode = ReportingInteractionMode.TOAST
)
class MyApplication : Application() {
    companion object {
        lateinit var prefs: MySharedPreferences
    }

    override fun onCreate() {
        super.onCreate()
        prefs = MySharedPreferences(applicationContext)
        ACRA.init(this);
        ACRA.getErrorReporter().setReportSender(LocalReportSender(this));
    }
}