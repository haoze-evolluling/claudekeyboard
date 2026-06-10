package com.haoze.claudekeyboard.macro

/**
 * Preset macros for Claude Code CLI.
 */
object PresetMacros {

    /**
     * Get the list of preset macros.
     */
    fun getPresets(): List<Macro> {
        return listOf(
            Macro.preset("/clear", "/clear\n", 1),
            Macro.preset("/exit", "/exit\n", 2),
            Macro.preset("/compact", "/compact\n", 3),
            Macro.preset("/cost", "/cost\n", 4),
            Macro.preset("/model", "/model\n", 5),
            Macro.preset("/help", "/help\n", 6)
        )
    }

    /**
     * Get a preset macro by label.
     */
    fun getPresetByLabel(label: String): Macro? {
        return getPresets().find { it.label == label }
    }

    /**
     * Check if a label is a preset macro label.
     */
    fun isPresetLabel(label: String): Boolean {
        return getPresets().any { it.label == label }
    }
}