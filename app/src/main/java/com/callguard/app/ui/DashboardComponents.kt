package com.callguard.app.ui

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callguard.app.data.local.FilterMode
import com.callguard.app.data.preferences.FilterSettings

@Composable
fun DashboardScreen(
    settings: FilterSettings,
    onModeSelected: (FilterMode) -> Unit,
    onToggleSetting: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        RolePermissionBanner(onRoleGranted = {})
        ModeSelectorCard(
            currentMode = settings.activeMode,
            onModeSelected = onModeSelected
        )

        Text(
            text = "Exceptions et règles globales",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        SettingSwitchRow(
            title = "Bloquer les numéros masqués",
            subtitle = "Rejette discrètement les appels privés ou inconnus",
            checked = settings.blockHiddenNumbers,
            onCheckedChange = { onToggleSetting("block_hidden", it) }
        )
        SettingSwitchRow(
            title = "Autoriser les contacts favoris",
            subtitle = "Les favoris passent même en mode Liste Blanche Strict",
            checked = settings.allowFavoriteContacts,
            onCheckedChange = { onToggleSetting("allow_favorites", it) }
        )
        SettingSwitchRow(
            title = "Autoriser tout le répertoire",
            subtitle = "Permet aux contacts enregistrés de passer en liste blanche",
            checked = settings.allowAllContactsInWhitelist,
            onCheckedChange = { onToggleSetting("allow_all_contacts", it) }
        )
        SettingSwitchRow(
            title = "Exception : appels répétés (5 min)",
            subtitle = "Autorise le 2e appel consécutif d'un même numéro",
            checked = settings.allowRepeatedCalls,
            onCheckedChange = { onToggleSetting("allow_repeated", it) }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun RolePermissionBanner(onRoleGranted: () -> Unit) {
    val context = LocalContext.current
    val roleManager = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        } else null
    }

    var isRoleHeld by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
                roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            } else false
        )
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isRoleHeld = true
            onRoleGranted()
        }
    }

    if (!isRoleHeld && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Autorisation de filtrage requise",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pour bloquer les appels discrètement tout en conservant votre application Téléphone actuelle, activez CallGuard comme service de filtrage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        roleLauncher.launch(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Activer le filtrage natif")
                }
            }
        }
    }
}

@Composable
fun ModeSelectorCard(
    currentMode: FilterMode,
    onModeSelected: (FilterMode) -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = when (currentMode) {
            FilterMode.WHITELIST -> Color(0xFF1B5E20).copy(alpha = 0.15f)
            FilterMode.BLACKLIST -> Color(0xFFB71C1C).copy(alpha = 0.15f)
            FilterMode.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "cardColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MODE DE PROTECTION ACTIF",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (currentMode) {
                    FilterMode.WHITELIST -> "Liste Blanche Strict"
                    FilterMode.BLACKLIST -> "Liste Noire Active"
                    FilterMode.DISABLED -> "Filtrage Désactivé"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(20.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = currentMode == FilterMode.WHITELIST,
                    onClick = { onModeSelected(FilterMode.WHITELIST) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                ) {
                    Text("Blanche", maxLines = 1)
                }
                SegmentedButton(
                    selected = currentMode == FilterMode.BLACKLIST,
                    onClick = { onModeSelected(FilterMode.BLACKLIST) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    icon = { Icon(Icons.Default.Block, contentDescription = null) }
                ) {
                    Text("Noire", maxLines = 1)
                }
                SegmentedButton(
                    selected = currentMode == FilterMode.DISABLED,
                    onClick = { onModeSelected(FilterMode.DISABLED) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    icon = { Icon(Icons.Default.Shield, contentDescription = null) }
                ) {
                    Text("Inactif", maxLines = 1)
                }
            }
        }
    }
}
