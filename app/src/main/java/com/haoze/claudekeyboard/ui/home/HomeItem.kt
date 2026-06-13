package com.haoze.claudekeyboard.ui.home

/**
 * Data types for the settings-style home page list.
 */
sealed class HomeItem {
    /**
     * Section header (e.g., "功能", "系统").
     */
    data class Section(val title: String) : HomeItem()

    /**
     * Clickable entry (e.g., Keyboard, Settings).
     */
    data class Entry(
        val id: String,
        val title: String,
        val subtitle: String,
        val iconRes: Int
    ) : HomeItem()
}
