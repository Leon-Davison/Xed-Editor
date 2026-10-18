*** Begin Patch
*** Update File: core/main/src/main/java/com/rk/tabs/editor/ExtraKeysByLanguage.kt
@@
-import android.content.Context
-import com.google.gson.Gson
-import com.google.gson.reflect.TypeToken
+import android.content.Context
+import com.google.gson.Gson
+import com.google.gson.reflect.TypeToken
+import com.rk.settings.Settings
+
+// Helper to merge defaults from assets with user overrides stored in Settings.per_language_extra_keys
+private fun mergeMaps(defaults: Map<String, ExtraKeysConfig>, overridesJson: String?): Map<String, ExtraKeysConfig> {
+    if (overridesJson.isNullOrBlank()) return defaults
+    return try {
+        val type = object : TypeToken<Map<String, ExtraKeysConfig>>() {}.type
+        val overrides: Map<String, ExtraKeysConfig> = Gson().fromJson(overridesJson, type) ?: emptyMap()
+        val merged = defaults.toMutableMap()
+        for ((k, v) in overrides) merged[k.lowercase()] = v
+        merged
+    } catch (e: Exception) {
+        defaults
+    }
+}
@@
-    private var cache: Map<String, ExtraKeysConfig>? = null
-
-    fun load(context: Context) {
-        if (cache != null) return
-        cache = runCatching {
-            context.assets.open("extra_keys_by_language.json").use { stream ->
-                val json = stream.bufferedReader().use { it.readText() }
-                val type = object : TypeToken<Map<String, ExtraKeysConfig>>() {}.type
-                gson.fromJson<Map<String, ExtraKeysConfig>>(json, type)
-            }
-        }.getOrElse { emptyMap<String, ExtraKeysConfig>() }
-    }
+    private var cache: Map<String, ExtraKeysConfig>? = null
+
+    fun load(context: Context) {
+        if (cache != null) return
+
+        val defaults = runCatching {
+            context.assets.open("extra_keys_by_language.json").use { stream ->
+                val json = stream.bufferedReader().use { it.readText() }
+                val type = object : TypeToken<Map<String, ExtraKeysConfig>>() {}.type
+                gson.fromJson<Map<String, ExtraKeysConfig>>(json, type) ?: emptyMap()
+            }
+        }.getOrElse { emptyMap<String, ExtraKeysConfig>() }
+
+        // merge with user overrides stored in Settings.per_language_extra_keys
+        val merged = mergeMaps(defaults, Settings.per_language_extra_keys)
+        cache = merged.mapKeys { it.key.lowercase() }
+    }
+
+    fun invalidate() {
+        cache = null
+    }
+
+    // Expose merged defaults for settings UI
+    fun getMergedDefaults(): Map<String, ExtraKeysConfig> {
+        load(rk.utils.application!!)
+        return cache ?: emptyMap()
+    }
*** End Patch
