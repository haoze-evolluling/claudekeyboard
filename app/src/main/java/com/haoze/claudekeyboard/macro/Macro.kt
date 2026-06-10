package com.haoze.claudekeyboard.macro

import java.util.UUID

/**
 * Data class representing a macro button configuration.
 */
data class Macro(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val command: String,
    val isPreset: Boolean = false,
    val sortOrder: Int = 0
) {
    companion object {
        /**
         * Create a preset macro.
         */
        fun preset(label: String, command: String, sortOrder: Int): Macro {
            return Macro(
                id = "preset_${label.replace("/", "_")}",
                label = label,
                command = command,
                isPreset = true,
                sortOrder = sortOrder
            )
        }

        /**
         * Create a custom macro.
         */
        fun custom(label: String, command: String): Macro {
            return Macro(
                label = label,
                command = command,
                isPreset = false,
                sortOrder = 1000 // Custom macros come after presets
            )
        }
    }
}