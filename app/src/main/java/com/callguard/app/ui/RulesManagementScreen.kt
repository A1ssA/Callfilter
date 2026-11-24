package com.callguard.app.ui

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.callguard.app.data.local.ListType
import com.callguard.app.data.local.PhoneRuleEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesManagementScreen(
    listType: ListType,
    rules: List<PhoneRuleEntity>,
    onAddOrUpdateRule: (PhoneRuleEntity) -> Unit,
    onDeleteRule: (PhoneRuleEntity) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var ruleBeingEdited by remember { mutableStateOf<PhoneRuleEntity?>(null) }

    var prefilledName by remember { mutableStateOf("") }
    var prefilledNumber by remember { mutableStateOf("") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        contactUri?.let { uri ->
            val extracted = extractContactDetails(context, uri)
            if (extracted != null) {
                prefilledName = extracted.first
                prefilledNumber = extracted.second
                ruleBeingEdited = null
                showEditDialog = true
            }
        }
    }

    val filteredRules = remember(rules, searchQuery) {
        if (searchQuery.isBlank()) rules
        else rules.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumber.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (listType == ListType.WHITELIST) "Liste Blanche (Autorisés)"
                        else "Liste Noire (Bloqués)"
                    )
                },
                actions = {
                    IconButton(onClick = { contactPickerLauncher.launch(null) }) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = "Importer un contact"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    prefilledName = ""
                    prefilledNumber = ""
                    ruleBeingEdited = null
                    showEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Ajouter un numéro") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Rechercher un nom ou un numéro...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true
            )

            if (filteredRules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun numéro enregistré dans cette liste.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    items(filteredRules, key = { it.id }) { rule ->
                        RuleItemCard(
                            rule = rule,
                            onEdit = {
                                ruleBeingEdited = rule
                                prefilledName = rule.displayName
                                prefilledNumber = rule.phoneNumber
                                showEditDialog = true
                            },
                            onDelete = { onDeleteRule(rule) }
                        )
                    }
                }
            }
        }

        if (showEditDialog) {
            AddOrEditRuleDialog(
                existingRule = ruleBeingEdited,
                initialName = prefilledName,
                initialNumber = prefilledNumber,
                listType = listType,
                onDismiss = { showEditDialog = false },
                onConfirm = { savedRule ->
                    onAddOrUpdateRule(savedRule)
                    showEditDialog = false
                }
            )
        }
    }
}

@Composable
private fun RuleItemCard(
    rule: PhoneRuleEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.displayName.ifBlank { "Sans nom" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = rule.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (rule.description.isNotBlank()) {
                    Text(
                        text = rule.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Modifier")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddOrEditRuleDialog(
    existingRule: PhoneRuleEntity?,
    initialName: String,
    initialNumber: String,
    listType: ListType,
    onDismiss: () -> Unit,
    onConfirm: (PhoneRuleEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var number by remember { mutableStateOf(initialNumber) }
    var description by remember { mutableStateOf(existingRule?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existingRule == null) "Ajouter un numéro" else "Modifier la règle")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Numéro de téléphone *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom / Libellé") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optionnel)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = number.isNotBlank(),
                onClick = {
                    onConfirm(
                        PhoneRuleEntity(
                            id = existingRule?.id ?: 0L,
                            phoneNumber = number.trim(),
                            displayName = name.trim().ifBlank { number.trim() },
                            description = description.trim(),
                            listType = listType
                        )
                    )
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

private fun extractContactDetails(context: Context, contactUri: Uri): Pair<String, String>? {
    return try {
        var contactId: String? = null
        var displayName = ""

        context.contentResolver.query(
            contactUri,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                contactId = cursor.getString(0)
                displayName = cursor.getString(1) ?: ""
            }
        }

        if (contactId != null) {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { phoneCursor ->
                if (phoneCursor.moveToFirst()) {
                    val phoneNumber = phoneCursor.getString(0) ?: ""
                    return Pair(displayName, phoneNumber)
                }
            }
        }
        null
    } catch (e: SecurityException) {
        null
    }
}
