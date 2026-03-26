package common.ui.pages.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val RyderAccent = Color(0xFF2678AB)

val LocalIsDarkTheme = compositionLocalOf { false }

object AppColors {
    val background: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFEEEEEE)

    val surface: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)

    val textPrimary: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFEEEEEE) else Color(0xFF1A1A1A)

    val textSecondary: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFAAAAAA) else Color(0xFF757575)

    val textHint: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF888888) else Color(0xFF9E9E9E)

    val divider: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2A) else Color(0xFFD9D9D9)

    val avatarPlaceholder: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF333333) else Color(0xFFD0D0D0)

    val tileBackground: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF252525) else Color(0xFFE8E8E8)

    val inputBorder: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF555555) else Color(0xFF9E9E9E)

    val tagBackground: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF333333) else Color(0xFFEEEEEE)
}
