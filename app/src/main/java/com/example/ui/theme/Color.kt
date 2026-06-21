package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val BentoPrimary = Color(0xFF386B3F)      // Deep Emerald Green
val BentoOnPrimary = Color(0xFFFFFFFF)    // Plain white text
val BentoSecondary = Color(0xFFE2F0D8)    // Soft Mint highlight/card background
val BentoOnSecondary = Color(0xFF191C19)  // Deep Charcoal/Forest text
val BentoTertiary = Color(0xFFD3E4FF)     // Soft Sky Blue for Water/Recovery
val BentoOnTertiary = Color(0xFF001D36)   // Deep navy for water
val BentoBg = Color(0xFFF7FBF2)           // Soft organic off-white
val BentoSurface = Color(0xFFFFFFFF)      // Clean card surface
val BentoOnSurface = Color(0xFF191C19)    // Carbon charcoal body text
val BentoSurfaceVariant = Color(0xFFE0E4DB) // Soft border outline color
val BentoOnSurfaceVariant = Color(0xFF414941) // Secondary label text

// Backwards compatibility so we don't break import references
val VoltPrimary = BentoPrimary
val VoltSecondary = BentoSecondary
val VoltTertiary = BentoTertiary
val VoltBg = BentoBg
val VoltSurface = BentoSurface
val OnVoltPrimary = BentoOnPrimary
val OnVoltSecondary = BentoOnSecondary
val VoltSurfaceVariant = BentoSurfaceVariant
