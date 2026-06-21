package com.haoze.claudekeyboard.macro

import android.content.Context
import com.haoze.claudekeyboard.R

/**
 * Preset macros for Claude Code CLI.
 */
object PresetMacros {

    /**
     * Get the list of preset macros.
     */
    fun getPresets(context: Context): List<Macro> {
        return listOf(
            Macro("preset__clear", "/clear", context.getString(R.string.preset_clear_desc), isPreset = true, sortOrder = 1, sendEnter = true),
            Macro("preset__compact", "/compact", context.getString(R.string.preset_compact_desc), isPreset = true, sortOrder = 2, sendEnter = true),
            Macro("preset__model", "/model", context.getString(R.string.preset_model_desc), isPreset = true, sortOrder = 3, sendEnter = true),
            Macro("preset__btw", "/btw", context.getString(R.string.preset_btw_desc), isPreset = true, sortOrder = 4, sendEnter = true)
        )
    }
}
