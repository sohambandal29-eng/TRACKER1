package com.example.tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tracker.ui.theme.GlassBorder
import com.example.tracker.ui.theme.GlassSurface
import com.example.tracker.ui.theme.PrimaryAccent

/**
 * Premium "Liquid Glass" card component.
 * Features multi-layered translucency, high-gloss edge lighting, and depth-simulating gradients.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    containerColor: Color = GlassSurface,
    alpha: Float = 1f, // Multiplier for containerColor.alpha
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.2.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    elevation: Dp = 12.dp,
    showAccentGlow: Boolean = false,
    isFloating: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "press_scale"
    )

    val borderAlphaMultiplier by animateFloatAsState(
        targetValue = if (isPressed) 1.5f else 1f,
        label = "border_alpha"
    )

    val finalAlpha = containerColor.alpha * alpha

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isFloating) elevation else 0.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        containerColor.copy(alpha = finalAlpha),
                        containerColor.copy(alpha = finalAlpha * if (alpha > 0.8f) 0.95f else 0.4f),
                        containerColor.copy(alpha = finalAlpha * if (alpha > 0.8f) 0.98f else 0.8f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f * borderAlphaMultiplier),
                        Color.White.copy(alpha = 0.05f),
                        if (showAccentGlow) PrimaryAccent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .pointerInput(onClick) {
                if (onClick != null) {
                    detectTapGestures(
                        onPress = { 
                            isPressed = true
                            try { awaitRelease() } finally { isPressed = false } 
                        },
                        onTap = { onClick() }
                    )
                }
            }
            .drawBehind {
                if (showAccentGlow) {
                    // Refracted inner glow for "Liquid" feel
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(PrimaryAccent.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.1f),
                            radius = size.maxDimension * 0.8f
                        ),
                        radius = size.maxDimension * 0.8f,
                        center = Offset(size.width * 0.1f, size.height * 0.1f)
                    )
                }
            }
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
