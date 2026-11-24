package com.callguard.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callguard.app.data.local.CallLogEntity
import com.callguard.app.data.local.FilterMode
import com.callguard.app.data.local.ScheduleEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesAndHistoryScreen(
    schedules: List<ScheduleEntity>,
    history: List<CallLogEntity>,
    onAddNightSchedule: () -> Unit,
    onToggleSchedule: (ScheduleEntity) -> Unit,
    onDeleteSchedule: (ScheduleEntity) -> Unit,
    onClearHistory: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Programmation (${schedules.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Historique (${history.size})") }
            )
        }

        if (selectedTab == 0) {
            Scaffold(
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = onAddNightSchedule,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Plage Nuit 22h-07h") }
                    )
                }
            ) { padding ->
                if (schedules.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucune plage horaire configurée.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(padding)
                    ) {
                        items(schedules, key = { it.id }) { schedule ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(schedule.label, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = String.format(
                                                Locale.FRANCE,
                                                "%02d:%02d → %02d:%02d (%s)",
                                                schedule.startHour,
                                                schedule.startMinute,
                                                schedule.endHour,
                                                schedule.endMinute,
                                                if (schedule.targetMode == FilterMode.WHITELIST) "Blanche" else "Noire"
                                            ),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Switch(
                                        checked = schedule.isEnabled,
                                        onCheckedChange = { onToggleSchedule(schedule.copy(isEnabled = it)) }
                                    )
                                    IconButton(onClick = { onDeleteSchedule(schedule) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if (history.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onClearHistory) {
                            Text("Vider l'historique")
                        }
                    }
                }
                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucun appel filtré enregistré.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history, key = { it.id }) { log ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = log.contactName ?: log.phoneNumber,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = dateFormatter.format(Date(log.timestamp)),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${if (log.wasBlocked) "Bloqué" else "Autorisé"} · ${log.reason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (log.wasBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
