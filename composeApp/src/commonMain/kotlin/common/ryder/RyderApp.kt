package common.ryder

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import common.ui.pages.*
import common.data.provideAuthService
import kotlinx.coroutines.launch
import ui.pages.components.NavBar
import common.ui.pages.Screen

@Composable
fun RyderApp() {

    val authService = provideAuthService()
    val scope = rememberCoroutineScope()

    val firebaseUser = authService.getCurrentUserId()

    var currentScreen by remember {
        mutableStateOf(
            if (firebaseUser != null) Screen.Home else Screen.Login
        )
    }

    var authError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = {
            if (currentScreen !in listOf(Screen.Registration, Screen.Login)) {
                NavBar(
                    currentScreen = currentScreen,
                    onHome = { currentScreen = Screen.Home },
                    onSearch = { currentScreen = Screen.Search },
                    onAddPost = { currentScreen = Screen.CreatePost }, // <-- new screen
                    onMessages = { currentScreen = Screen.Messages },
                    onProfile = { currentScreen = Screen.Profile }
                )
            }
        }
    ) { padding ->

        when (currentScreen) {

            Screen.Registration -> RegistrationPage(
                backendError = authError,
                onRegister = { email, password, nickname, firstName, lastName ->

                    scope.launch {

                        val result = authService.register(
                            email,
                            password,
                            nickname,
                            firstName,
                            lastName
                        )

                        if (result.isSuccess) {
                            authError = null
                            currentScreen = Screen.Home
                        } else {
                            authError = result.exceptionOrNull()?.message
                        }
                    }
                }
            )

            Screen.Login -> LoginPage(
                backendError = authError,
                onLogin = { email, password ->

                    scope.launch {
                        val result = authService.login(email, password)

                        if (result.isSuccess) {
                            authError = null
                            currentScreen = Screen.Home
                        } else {
                            authError = result.exceptionOrNull()?.message
                        }
                    }
                },
                onForgotPassword = { email ->

                    scope.launch {
                        val result = authService.sendPasswordReset(email)

                        authError = if (result.isSuccess) {
                            "Password reset email sent."
                        } else {
                            result.exceptionOrNull()?.message
                        }
                    }
                }
            )

            Screen.Home -> Homepage(
                onLoginClick = { currentScreen = Screen.Login },
                onRegisterClick = { currentScreen = Screen.Registration }
            )

            Screen.Search -> SearchPage()
            Screen.Messages -> MessagesPage()
            Screen.Profile -> ProfilePage(authService = provideAuthService())
        }
    }
}
