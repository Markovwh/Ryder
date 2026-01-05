package common.ryder

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import common.ui.pages.Homepage
import androidx.compose.runtime.Composable

@Composable
fun RyderApp() {

    // UI dizaina mainīgie
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6200EE),
            onPrimary = Color.White,
            background = Color(0xFFF2F2F2)
        )
    ) {

        // Liek lapai aizpildīt visu ekrānu
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {

            // Ielādē sākumlapu
            Homepage()
        }
    }
}
