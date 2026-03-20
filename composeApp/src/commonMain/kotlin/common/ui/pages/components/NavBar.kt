package ui.pages.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import common.ui.pages.Screen
import common.ui.pages.components.RyderAccent

@Composable
fun NavBar(
    currentScreen: Screen,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onAddPost: () -> Unit,
    onMessages: () -> Unit,
    onProfile: () -> Unit
) {
    BottomAppBar(containerColor = Color(0xFFF5F5F5)) {

        NavItem(
            selected = currentScreen == Screen.Home,
            icon = Icons.Default.Home,
            onClick = onHome
        )

        NavItem(
            selected = currentScreen == Screen.Search,
            icon = Icons.Default.Search,
            onClick = onSearch
        )

        AddPostButton(onClick = onAddPost)

        NavItem(
            selected = currentScreen == Screen.Messages,
            icon = Icons.Default.Mail,
            onClick = onMessages
        )

        NavItem(
            selected = currentScreen == Screen.Profile,
            icon = Icons.Default.Person,
            onClick = onProfile
        )
    }
}

@Composable
private fun RowScope.AddPostButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(RyderAccent, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Jauna ziņa",
                tint = Color(0xFF1A1A1A),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) RyderAccent else Color.Transparent,
        animationSpec = spring(),
        label = ""
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = ""
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(iconScale)
                .background(backgroundColor, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color(0xFF1A1A1A) else Color(0xFF757575)
            )
        }
    }
}
