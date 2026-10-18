package com.rk.settings.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rk.components.compose.preferences.base.PreferenceScaffold
import com.rk.resources.strings
import com.rk.settings.Settings
import com.rk.settings.Preference
import com.rk.tabs.editor.ExtraKeysByLanguage
import com.rk.tabs.editor.ExtraKeysConfig

@Composable
fun EditPerLanguageExtraKeys(onClose: () -> Unit = {}) {
    val gson = remember { Gson() }
    val defaults = remember { ExtraKeysByLanguage.getMergedDefaults() }
    var entriesState = remember { mutableStateOf(defaults.toList().toMap().toMutableMap()) }

    var editing by remember { mutableStateOf<Pair<String, ExtraKeysConfig>?>(null) }
    var adding by remember { mutableStateOf(false) }

    fun saveEntries() {
        val map = entriesState.value.toMap()
        Preference.setString("per_language_extra_keys", gson.toJson(map))
        ExtraKeysByLanguage.invalidate()
    }

    PreferenceScaffold(
        label = "Per-language extra keys",
        backArrowVisible = true,
        actions = {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                Button(
                    onClick = { adding = true },
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text("Add language")
                }
            }

            items(entriesState.value.toList()) { (language, data) ->
                androidx.compose.material3.Card(
                    modifier = Modifier.padding(4.dp),
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        androidx.compose.material3.Text(language)
                        androidx.compose.material3.Text("symbols: ${data.symbols}")
                        androidx.compose.material3.Text("commands: ${data.commands}")
                        IconButton(onClick = { editing = language to data }) {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                        }
                        IconButton(onClick = {
                            entriesState.value = entriesState.value.apply { remove(language) }
                            saveEntries()
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        var language by remember { mutableStateOf("") }
        var symbols by remember { mutableStateOf("") }
        var commands by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Add language override") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Language key") })
                    }
                    item {
                        OutlinedTextField(value = symbols, onValueChange = { symbols = it }, label = { Text("Symbols") })
                    }
                    item {
                        OutlinedTextField(value = commands, onValueChange = { commands = it }, label = { Text("Commands") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (language.isNotBlank()) {
                            entriesState.value = entriesState.value.apply {
                                put(language.lowercase(), ExtraKeysConfig(symbols, commands))
                            }
                            saveEntries()
                        }
                        adding = false
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { adding = false }) { Text("Cancel") }
            },
        )
    }

    editing?.let { (language, data) ->
        var symbolsValue by remember { mutableStateOf(data.symbols ?: "") }
        var commandsValue by remember { mutableStateOf(data.commands ?: "") }

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit $language") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(value = symbolsValue, onValueChange = { symbolsValue = it }, label = { Text("Symbols") })
                    }
                    item {
                        OutlinedTextField(value = commandsValue, onValueChange = { commandsValue = it }, label = { Text("Commands") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        entriesState.value = entriesState.value.apply {
                            put(language, ExtraKeysConfig(symbolsValue, commandsValue))
                        }
                        saveEntries()
                        editing = null
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { editing = null }) { Text("Cancel") }
            },
        )
    }
}
