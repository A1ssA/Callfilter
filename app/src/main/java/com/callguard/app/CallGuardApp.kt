package com.callguard.app

import android.app.Application
import androidx.room.Room
import com.callguard.app.data.local.AppDatabase
import com.callguard.app.data.preferences.SettingsRepository
import com.callguard.app.domain.CallEvaluationEngine
import com.callguard.app.scheduler.ScheduleManager

class CallGuardApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var evaluationEngine: CallEvaluationEngine
        private set

    lateinit var scheduleManager: ScheduleManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "callguard.db"
        ).fallbackToDestructiveMigration().build()

        settingsRepository = SettingsRepository(applicationContext)
        evaluationEngine = CallEvaluationEngine(
            context = applicationContext,
            dao = database.callGuardDao(),
            settingsRepo = settingsRepository
        )
        scheduleManager = ScheduleManager(applicationContext)
    }
}
