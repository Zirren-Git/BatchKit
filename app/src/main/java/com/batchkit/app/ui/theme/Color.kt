package com.batchkit.app.ui.theme

import androidx.compose.ui.graphics.Color

// BatchKit palette: a calm blue-grey base with a single accent that reads well in
// both themes. Dynamic colour (Android 12+) overrides these when available.
val Blue40 = Color(0xFF1B6FBF)
val Blue80 = Color(0xFFA3CCF5)
val Teal40 = Color(0xFF1F7A6B)
val Teal80 = Color(0xFF8FD8CB)
val Slate10 = Color(0xFF10161C)
val Slate20 = Color(0xFF1B242E)
val Slate90 = Color(0xFFE7EEF5)
val Slate95 = Color(0xFFF4F8FC)
</EOF

cat > app/src/main/java/com/batchkit/app/ui/theme/Type.kt <<'EOF'
package com.batchkit.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val BatchKitTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    ),
)
