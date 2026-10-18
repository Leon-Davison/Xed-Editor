package com.rk.tabs.editor

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rk.settings.Settings
import com.rk.utils.application

// Simple config holder for per-language extra keys
data class ExtraKeysConfig(
    val symbols: String? = null,
    val commands: String? = null,
)

object ExtraKeysByLanguage {
    private val gson = Gson()
    private var cache: Map<String, ExtraKeysConfig>? = null

    // Helper to merge defaults from assets with user overrides stored in Settings.per_language_extra_keys
    private fun mergeMaps(defaults: Map<String, ExtraKeysConfig>, overridesJson: String?): Map<String, ExtraKeysConfig> {
        if (overridesJson.isNullOrBlank()) return defaults
        return try {
            val type = object : TypeToken<Map<String, ExtraKeysConfig>>() {}.type
            val overrides: Map<String, ExtraKeysConfig> = Gson().fromJson(overridesJson, type) ?: emptyMap()
            val merged = defaults.toMutableMap()
            for ((k, v) in overrides) merged[k.lowercase()] = v
            merged
        } catch (e: Exception) {
            defaults
        }
    }

    fun load(context: Context) {
        if (cache != null) return

        val defaults = runCatching {
            context.assets.open("extra_keys_by_language.json").use { stream ->
                val json = stream.bufferedReader().use { it.readText() }
                val type = object : TypeToken<Map<String, ExtraKeysConfig>>() {}.type
                gson.fromJson<Map<String, ExtraKeysConfig>>(json, type) ?: emptyMap()
            }
        }.getOrElse { emptyMap<String, ExtraKeysConfig>() }

        // merge with user overrides stored in Settings.per_language_extra_keys
        val merged = mergeMaps(defaults, Settings.per_language_extra_keys)
        cache = merged.mapKeys { it.key.lowercase() }
    }

    fun invalidate() {
        cache = null
    }

    // Expose merged defaults for settings UI
    fun getMergedDefaults(): Map<String, ExtraKeysConfig> {
        load(application!!)
        return cache ?: emptyMap()
    }

    fun getForTab(context: Context, editorTab: EditorTab): ExtraKeysConfig? {
        load(context)

        val candidates = mutableListOf<String>()

        editorTab.file?.getExtension()?.lowercase()?.let { candidates.add(it) }
        editorTab.file?.getName()?.substringAfterLast('.', "")?.lowercase()?.let { candidates.add(it) }

        editorTab.editorState.textmateScope?.lowercase()?.let { candidates.add(it) }
        editorTab.editorState.textmateScope?.substringAfterLast('.')?.lowercase()?.let { candidates.add(it) }
        editorTab.editorState.textmateScope?.substringAfter("source.", "")?.lowercase()?.let { candidates.add(it) }
        editorTab.editorState.textmateScope?.substringAfter("text.", "")?.lowercase()?.let { candidates.add(it) }

        candidates.add(editorTab.fallbackExtension.lowercase())

        // try direct then normalized versions
        for (candidate in candidates) {
            if (candidate.isBlank()) continue
            cache?.get(candidate)?.let { return it }
            val norm = candidate.replace(Regex("[^a-z0-9]"), "")
            if (norm.isNotBlank()) cache?.get(norm)?.let { return it }
        }

        return cache?.get("default")
    }
}
