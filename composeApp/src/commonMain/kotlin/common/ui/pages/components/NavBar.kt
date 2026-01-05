package ui.pages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val RyderRed = Color(0xFFD32F2F)

// Navigācijas joslas elements
@Composable
fun NavBar(
    onHomeClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCreateClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    // navigācijas joslas dizains
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color.Black),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // nosaka navigācijas joslas elementus
        IconButton(onClick = onHomeClick) {
            Icon(Icons.Filled.Home, contentDescription = "Home", tint = RyderRed)
        }

        IconButton(onClick = onSearchClick) {
            Icon(Icons.Filled.Search, contentDescription = "Search", tint = RyderRed)
        }

        IconButton(onClick = onCreateClick) {
            Icon(Icons.Filled.AddCircle, contentDescription = "Create", tint = RyderRed)
        }

        IconButton(onClick = onMessagesClick) {
            Icon(Icons.Filled.Email, contentDescription = "Messages", tint = RyderRed)
        }

        IconButton(onClick = onProfileClick) {
            Icon(Icons.Filled.Person, contentDescription = "Profile", tint = RyderRed)
        }
    }
}
