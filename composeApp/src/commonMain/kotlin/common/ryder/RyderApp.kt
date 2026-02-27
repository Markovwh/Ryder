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


    var currentScreen: Screen by remember { mutableStateOf(Screen.Login) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isGuest by remember { mutableStateOf(false) } // track guest mode

    Scaffold(
        bottomBar = {
            if (currentScreen !in listOf(Screen.Registration, Screen.Login)) {
                NavBar(
                    currentScreen = currentScreen,
                    onHome = { currentScreen = Screen.Home },
                    onSearch = { currentScreen = Screen.Search },
                    onAddPost = {
                        // Only allow if logged in
                        if (!isGuest && authService.getCurrentUserId() != null) {
                            currentScreen = Screen.CreatePost
                        } else {
                            currentScreen = Screen.Login
                        }
                    },
                    onMessages = {
                        if (!isGuest && authService.getCurrentUserId() != null) {
                            currentScreen = Screen.Messages
                        } else {
                            currentScreen = Screen.Login
                        }
                    },
                    onProfile = {
                        if (!isGuest && authService.getCurrentUserId() != null) {
                            currentScreen = Screen.Profile
                        } else {
                            currentScreen = Screen.Login
                        }
                    }
                )
            }
        }
    ) { padding ->

        when (currentScreen) {

            Screen.Registration -> RegistrationPage(
                backendError = authError,
                onRegister = { email, password, nickname, firstName, lastName ->
                    scope.launch {
                        val result = authService.register(email, password, nickname, firstName, lastName)
                        if (result.isSuccess) {
                            authError = null
                            currentScreen = Screen.Login
                        } else {
                            authError = result.exceptionOrNull()?.message
                        }
                    }
                },
                onLoginClick = { currentScreen = Screen.Login }
            )

            Screen.Login -> LoginPage(
                backendError = authError,
                onLogin = { email, password, rememberMe ->
                    scope.launch {
                        val result = authService.login(email, password)

                        if (result.isSuccess) {
                            // TODO: handle rememberMe, e.g., store in DataStore
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
                },
                onRegisterClick = { currentScreen = Screen.Registration },
                onContinueAsGuest = { currentScreen = Screen.Home }
            )

            Screen.Home -> Homepage(
                onLoginClick = { currentScreen = Screen.Login },
                onRegisterClick = { currentScreen = Screen.Registration },
                isUserLoggedIn = authService.getCurrentUserId() != null && !isGuest
            )

            Screen.Search -> SearchPage()

            Screen.Messages -> {
                // redirect guest to login
                if (!isGuest && authService.getCurrentUserId() != null) {
                    MessagesPage()
                } else {
                    currentScreen = Screen.Login
                }
            }

            Screen.Profile -> {
                if (!isGuest && authService.getCurrentUserId() != null) {
                    ProfilePage(
                        authService = authService,
                        onLogout = {
                            authService.logout()
                            isGuest = false
                            currentScreen = Screen.Login
                        }
                    )
                } else {
                    currentScreen = Screen.Login
                }
            }

            Screen.CreatePost -> {
                val currentUserId = authService.getCurrentUserId()
                if (!isGuest && currentUserId != null) {
                    val dummyUser = common.model.User(
                        nickname = "CurrentUser",
                        firstName = "First",
                        lastName = "Last",
                        profilePicture = null
                    )
                    CreatePostScreen(
                        currentUser = dummyUser,
                        onPostCreated = { currentScreen = Screen.Home },
                        onCancel = { currentScreen = Screen.Home }
                    )
                } else {
                    currentScreen = Screen.Login
                }
            }
        }
    }
}
