package com.sunflower.shortcut.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ShortcutShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Extra tokens used directly by components that need a radius Shapes doesn't cover.
object ShortcutRadius {
    val card = RoundedCornerShape(18.dp)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val button = RoundedCornerShape(16.dp)
    val chip = RoundedCornerShape(10.dp)
    val pill = RoundedCornerShape(50)
}
