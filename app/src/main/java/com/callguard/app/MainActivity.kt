package com.callguard.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.callguard.app.data.local.FilterMode
import com.callguard.app.data.local.ListType
import com.callguard.app.data.local.ScheduleEntity
import com.callguard.app.data.preferences.FilterSettings
import com.callguard.app.ui.DashboardScreen
import com.callguard.app.ui.RulesManagementScreen
import com.callguard.app.ui.SchedulesAndHistoryScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_PHONE_STATE
            )
        )

        val app = application as CallGuardApp
        val dao = app.database.callGuardDao()
        val settingsRepo = app.settingsRepository
        val scheduleManager = app.scheduleManager

        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                var currentTab by remember { mutableIntStateOf(0) }

                val settings by settingsRepo.settingsFlow.collectAsState(initial = FilterSettings())
                val whitelist by dao.getRulesByType(ListType.WHITELIST).collectAsState(initial = emptyList())
                val blacklist by dao.getRulesByType(ListType.BLACKLIST).collectAsState(initial = emptyList())
                val schedules by dao.getAllSchedules().collectAsState(initial = emptyList())
                val history by dao.getCallHistory().collectAsState(initial = emptyList())

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Default.Shield, contentDescription = null) },
                                label = { Text("Accueil") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                label = { Text("Blanche") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = { Icon(Icons.Default.Block, contentDescription = null) },
                                label = { Text("Noire") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = { Icon(Icons.Default.History, contentDescription = null) },
                                label = { Text("Horaires") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            0 -> DashboardScreen(
                                settings = settings,
                                onModeSelected = { mode ->
                                    scope.launch { settingsRepo.setFilterMode(mode) }
                                },
                                onToggleSetting = { key, value ->
                                    scope.launch { settingsRepo.updateBooleanSetting(key, value) }
                                }
                            )

                            1 -> RulesManagementScreen(
                                listType = ListType.WHITELIST,
                                rules = whitelist,
                                onAddOrUpdateRule = { rule ->
                                    scope.launch { dao.insertRule(rule) }
                                },
                                onDeleteRule = { rule ->
                                    scope.launch { dao.deleteRule(rule) }
                                }
                            )

                            2 -> RulesManagementScreen(
                                listType = ListType.BLACKLIST,
                                rules = blacklist,
                                onAddOrUpdateRule = { rule ->
                                    scope.launch { dao.insertRule(rule) }
                                },
                                onDeleteRule = { rule ->
                                    scope.launch { dao.deleteRule(rule) }
                                }
                            )

                            3 -> SchedulesAndHistoryScreen(
                                schedules = schedules,
                                history = history,
                                onAddNightSchedule = {
                                    scope.launch {
                                        val schedule = ScheduleEntity(
                                            label = "Mode Nuit Strict",
                                            targetMode = FilterMode.WHITELIST,
                                            fallbackMode = FilterMode.BLACKLIST,
                                            startHour = 22,
                                            startMinute = 0,
                                            endHour = 7,
                                            endMinute = 0,
                                            daysOfWeekMask = 127,
                                            isEnabled = true
                                        )
                                        val id = dao.insertSchedule(schedule)
                                        scheduleManager.scheduleAlarmsForRule(schedule.copy(id = id))
                                    }
                                },
                                onToggleSchedule = { updated ->
                                    scope.launch {
                                        dao.updateSchedule(updated)
                                        if (updated.isEnabled) {
                                            scheduleManager.scheduleAlarmsForRule(updated)
                                        } else {
                                            scheduleManager.cancelAlarmsForRule(updated.id)
                                        }
                                    }
                                },
                                onDeleteSchedule = { rule ->
                                    scope.launch {
                                        scheduleManager.cancelAlarmsForRule(rule.id)
                                        dao.deleteSchedule(rule)
                                    }
                                },
                                onClearHistory = {
                                    scope.launch { dao.clearCallHistory() }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
