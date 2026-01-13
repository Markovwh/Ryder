package ui.pages.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import common.ui.pages.components.RyderRed


// Navigācijas joslas elements
@Composable
fun NavBar(
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onMessages: () -> Unit,
    onProfile: () -> Unit
) {
    BottomAppBar(containerColor = Color.Black) {

        IconButton(onClick = onHome, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Home, null, tint = RyderRed)
        }

        IconButton(onClick = onSearch, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Search, null, tint = Color.White)
        }

        IconButton(onClick = onMessages, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Mail, null, tint = Color.White)
        }

        IconButton(onClick = onProfile, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Person, null, tint = Color.White)
        }
    }
}
