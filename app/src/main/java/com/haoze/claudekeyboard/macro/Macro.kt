package com.haoze.claudekeyboard.macro

import java.util.UUID

data class Macro(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val command: String,
    val description: String = "",
    val isPreset: Boolean = false,
    val sortOrder: Int = 0,
    val sendEnter: Boolean = false
)