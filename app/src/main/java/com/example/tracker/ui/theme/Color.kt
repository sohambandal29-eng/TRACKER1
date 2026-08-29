package com.example.tracker.ui.theme

import androidx.compose.ui.graphics.Color

// --- Core Palette ---
val BackgroundDark = Color(0xFF08080A)
val SurfaceDark = Color(0xFF111114)
val SurfaceVariantDark = Color(0xFF1A1A1E)

// --- The ONE Primary Accent (Electric Violet) ---
val PrimaryAccent = Color(0xFF8B5CF6) // Vibrant Violet
val PrimaryLight = Color(0xFFA78BFA)
val PrimaryDark = Color(0xFF6D28D9)

// --- Secondary / Neutral ---
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.24f)
val GlassSurface = Color(0xFFFFFFFF).copy(alpha = 0.1f)

// --- Functional ---
val SuccessGreen = Color(0xFF10B981)
val ErrorRed = Color(0xFFEF4444)
val WarningOrange = Color(0xFFF59E0B)

// --- Accent Palette ---
val SecondaryCyan = Color(0xFF22D3EE)
val PrimaryBlue = Color(0xFF3B82F6)
val AccentPurple = Color(0xFFC084FC)

// --- Gradients ---
val GradientStart = PrimaryAccent
val GradientEnd = AccentPurple
val PrimaryGradient = listOf(PrimaryAccent, AccentPurple)
val SecondaryGradient = listOf(SecondaryCyan, PrimaryLight)
val DarkGradient = listOf(Color(0xFF111114), Color(0xFF08080A))
val GlassGradient = listOf(
    Color.White.copy(alpha = 0.1f),
    Color.White.copy(alpha = 0.02f)
)
