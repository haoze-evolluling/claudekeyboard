package com.haoze.claudekeyboard.macro

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository for managing macros (preset + custom).
 * Uses SharedPreferences with JSON serialization.
 */
class MacroRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "macro_prefs"
        private const val KEY_MACROS = "macros"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Get all macros (presets first, then customs).
     */
    fun getAllMacros(): List<Macro> {
        val storedMacros = loadMacrosFromStorage()

        // If no stored macros, initialize with presets
        if (storedMacros.isEmpty()) {
            val presets = PresetMacros.getPresets()
            saveMacrosToStorage(presets)
            return presets
        }

        // Ensure presets are always present
        val presets = PresetMacros.getPresets()
        val customs = storedMacros.filter { !it.isPreset }

        return presets + customs
    }

    /**
     * Get only custom macros.
     */
    fun getCustomMacros(): List<Macro> {
        return getAllMacros().filter { !it.isPreset }
    }

    /**
     * Add a custom macro.
     */
    fun addCustomMacro(label: String, command: String): Macro {
        val macro = Macro.custom(label, command)
        val macros = getAllMacros().toMutableList()
        macros.add(macro)
        saveMacrosToStorage(macros)
        return macro
    }

    /**
     * Update a custom macro.
     */
    fun updateCustomMacro(id: String, label: String, command: String): Boolean {
        val macros = getAllMacros().toMutableList()
        val index = macros.indexOfFirst { it.id == id && !it.isPreset }

        if (index == -1) return false

        macros[index] = macros[index].copy(label = label, command = command)
        saveMacrosToStorage(macros)
        return true
    }

    /**
     * Delete a custom macro.
     */
    fun deleteCustomMacro(id: String): Boolean {
        val macros = getAllMacros().toMutableList()
        val removed = macros.removeAll { it.id == id && !it.isPreset }

        if (removed) {
            saveMacrosToStorage(macros)
        }

        return removed
    }

    /**
     * Get a macro by ID.
     */
    fun getMacroById(id: String): Macro? {
        return getAllMacros().find { it.id == id }
    }

    /**
     * Load macros from SharedPreferences.
     */
    private fun loadMacrosFromStorage(): List<Macro> {
        val jsonString = prefs.getString(KEY_MACROS, null) ?: return emptyList()

        return try {
            val jsonArray = JSONArray(jsonString)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                Macro(
                    id = obj.getString("id"),
                    label = obj.getString("label"),
                    command = obj.getString("command"),
                    isPreset = obj.getBoolean("isPreset"),
                    sortOrder = obj.getInt("sortOrder")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Save macros to SharedPreferences.
     */
    private fun saveMacrosToStorage(macros: List<Macro>) {
        val jsonArray = JSONArray()
        macros.forEach { macro ->
            val obj = JSONObject().apply {
                put("id", macro.id)
                put("label", macro.label)
                put("command", macro.command)
                put("isPreset", macro.isPreset)
                put("sortOrder", macro.sortOrder)
            }
            jsonArray.put(obj)
        }

        prefs.edit()
            .putString(KEY_MACROS, jsonArray.toString())
            .apply()
    }

    /**
     * Reset to default presets (clear all custom macros).
     */
    fun resetToDefaults() {
        val presets = PresetMacros.getPresets()
        saveMacrosToStorage(presets)
    }
}