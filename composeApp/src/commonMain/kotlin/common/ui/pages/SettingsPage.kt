@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.data.AuthService
import common.model.User
import common.ui.pages.components.AppColors
import common.ui.pages.components.RyderAccent

@Composable
fun SettingsPage(
    authService: AuthService,
    currentUser: User?,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onOpenAdmin: (() -> Unit)? = null
) {
    val bg = AppColors.background
    val surface = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val dividerColor = AppColors.divider
    Column(modifier = Modifier.fillMaxSize().background(bg)) {
        TopAppBar(
            title = { Text("Iestatījumi", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Atpakaļ", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = RyderAccent)
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionHeader(text = "Konts", textColor = textSecondary)

        Column(modifier = Modifier.fillMaxWidth().background(surface)) {
            SettingsRow(
                icon = Icons.Default.Edit,
                label = "Rediģēt profilu",
                textColor = textPrimary,
                iconColor = RyderAccent,
                onClick = onEditProfile
            )
            HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 56.dp))
            SettingsRow(
                icon = Icons.Default.ExitToApp,
                label = "Iziet",
                textColor = Color(0xFFE53935),
                iconColor = Color(0xFFE53935),
                onClick = {
                    authService.logout()
                    onLogout()
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionHeader(text = "Izskats", textColor = textSecondary)

        Column(modifier = Modifier.fillMaxWidth().background(surface)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = RyderAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Tumšais režīms",
                    color = textPrimary,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = onToggleDarkTheme,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RyderAccent
                    )
                )
            }
        }

        // Admin section (visible to admins only)
        if (currentUser?.isAdmin == true && onOpenAdmin != null) {
            Spacer(modifier = Modifier.height(12.dp))

            SettingsSectionHeader(text = "Administrācija", textColor = textSecondary)

            Column(modifier = Modifier.fillMaxWidth().background(surface)) {
                SettingsRow(
                    icon = Icons.Default.AdminPanelSettings,
                    label = "Administratora panelis",
                    textColor = textPrimary,
                    iconColor = RyderAccent,
                    onClick = onOpenAdmin
                )
            }
        }

    }
}

@Composable
private fun SettingsSectionHeader(text: String, textColor: Color) {
    Text(
        text = text.uppercase(),
        color = textColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    textColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, color = textColor, fontSize = 16.sp)
    }
}
