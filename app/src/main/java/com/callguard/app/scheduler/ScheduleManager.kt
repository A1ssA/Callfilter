package com.callguard.app.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.callguard.app.CallGuardApp
import com.callguard.app.data.local.FilterMode
import com.callguard.app.data.local.ScheduleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ScheduleManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val ACTION_TRIGGER_SCHEDULE = "com.callguard.app.ACTION_TRIGGER_SCHEDULE"
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
        const val EXTRA_TARGET_MODE = "extra_target_mode"
        const val EXTRA_IS_START = "extra_is_start"
    }

    fun scheduleAlarmsForRule(schedule: ScheduleEntity) {
        cancelAlarmsForRule(schedule.id)
        if (!schedule.isEnabled || schedule.daysOfWeekMask == 0) return

        val nextStartMillis = computeNextOccurrenceMillis(
            schedule.startHour,
            schedule.startMinute,
            schedule.daysOfWeekMask
        )
        setExactAlarm(
            requestCode = (schedule.id * 10 + 1).toInt(),
            triggerAtMillis = nextStartMillis,
            scheduleId = schedule.id,
            modeToApply = schedule.targetMode,
            isStart = true
        )

        val nextEndMillis = computeNextOccurrenceMillis(
            schedule.endHour,
            schedule.endMinute,
            schedule.daysOfWeekMask
        )
        setExactAlarm(
            requestCode = (schedule.id * 10 + 2).toInt(),
            triggerAtMillis = nextEndMillis,
            scheduleId = schedule.id,
            modeToApply = schedule.fallbackMode,
            isStart = false
        )
    }

    fun cancelAlarmsForRule(scheduleId: Long) {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_SCHEDULE
        }
        val startPending = PendingIntent.getBroadcast(
            context, (scheduleId * 10 + 1).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val endPending = PendingIntent.getBroadcast(
            context, (scheduleId * 10 + 2).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(startPending)
        alarmManager.cancel(endPending)
    }

    private fun setExactAlarm(
        requestCode: Int,
        triggerAtMillis: Long,
        scheduleId: Long,
        modeToApply: FilterMode,
        isStart: Boolean
    ) {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_SCHEDULE
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_TARGET_MODE, modeToApply.name)
            putExtra(EXTRA_IS_START, isStart)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun computeNextOccurrenceMillis(hour: Int, minute: Int, daysMask: Int): Long {
        val now = Calendar.getInstance()
        val candidate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (dayOffset in 0..7) {
            val checkCal = (candidate.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            if (checkCal.timeInMillis <= now.timeInMillis) continue

            val calDay = checkCal.get(Calendar.DAY_OF_WEEK)
            val bitIndex = if (calDay == Calendar.SUNDAY) 6 else calDay - 2
            if ((daysMask and (1 shl bitIndex)) != 0) {
                return checkCal.timeInMillis
            }
        }
        return candidate.timeInMillis + 24 * 60 * 60 * 1000L
    }
}

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as CallGuardApp
        val scheduleManager = ScheduleManager(context)

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val activeSchedules = app.database.callGuardDao().getEnabledSchedulesSync()
                    activeSchedules.forEach { scheduleManager.scheduleAlarmsForRule(it) }
                }
            }

            ScheduleManager.ACTION_TRIGGER_SCHEDULE -> {
                val scheduleId = intent.getLongExtra(ScheduleManager.EXTRA_SCHEDULE_ID, -1L)
                val modeName = intent.getStringExtra(ScheduleManager.EXTRA_TARGET_MODE) ?: return
                val targetMode = FilterMode.valueOf(modeName)

                CoroutineScope(Dispatchers.IO).launch {
                    app.settingsRepository.setFilterMode(targetMode)
                    val schedule = app.database.callGuardDao().getEnabledSchedulesSync()
                        .find { it.id == scheduleId }
                    schedule?.let { scheduleManager.scheduleAlarmsForRule(it) }
                }
            }
        }
    }
}
