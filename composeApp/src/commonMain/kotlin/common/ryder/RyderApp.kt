package common.ryder

import androidx.compose.material3.Scaffold
import common.ui.pages.Homepage
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import common.ui.pages.LoginPage
import common.ui.pages.MessagesPage
import common.ui.pages.ProfilePage
import common.ui.pages.RegistrationPage
import common.ui.pages.SearchPage
import ui.pages.components.NavBar

@Composable
fun RyderApp() {

    var currentScreen by remember { mutableStateOf(Screen.Home) }

    Scaffold(
        bottomBar = {
            if (currentScreen !in listOf(Screen.Registration, Screen.Login)) {
                NavBar(
                    onHome = { currentScreen = Screen.Home },
                    onSearch = { currentScreen = Screen.Search },
                    onMessages = { currentScreen = Screen.Messages },
                    onProfile = { currentScreen = Screen.Profile }
                )
            }
        }
    ) { padding ->

        when (currentScreen) {
            Screen.Registration ->
                RegistrationPage(onContinue = { currentScreen = Screen.Login })

            Screen.Login ->
                LoginPage(onLogin = { currentScreen = Screen.Home })

            Screen.Home -> Homepage()
            Screen.Search -> SearchPage()
            Screen.Messages -> MessagesPage()
            Screen.Profile -> ProfilePage()
        }
    }
}
