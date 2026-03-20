package common.ryder

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import common.ui.pages.*
import common.data.provideAuthService
import common.data.UserPreferences
import common.model.User
import kotlinx.coroutines.launch
import ui.pages.components.NavBar
import common.ui.pages.Screen

@Composable
fun RyderApp(userPreferences: UserPreferences? = null) {

    val authService = provideAuthService()
    val scope = rememberCoroutineScope()

    var currentScreen: Screen by remember { mutableStateOf(Screen.Login) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isGuest by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<User?>(null) }

    fun loadCurrentUser() {
        scope.launch {
            val uid = authService.getCurrentUserId() ?: return@launch
            val result = authService.getUserData(uid)
            if (result.isSuccess) currentUser = result.getOrNull()
        }
    }

    // On startup: if remember me was enabled and Firebase still has an active session, skip login
    LaunchedEffect(Unit) {
        if (userPreferences != null) {
            val rememberMe = userPreferences.getRememberMe()
            if (rememberMe && authService.getCurrentUserId() != null) {
                loadCurrentUser()
                currentScreen = Screen.Home
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (currentScreen !in listOf(Screen.Registration, Screen.Login, Screen.EditProfile, Screen.CreateGroup, Screen.CreateEvent, Screen.Admin)
                && currentScreen !is Screen.Chat
                && currentScreen !is Screen.UserProfile
                && currentScreen !is Screen.HashtagFeed
                && currentScreen !is Screen.GroupDetail
                && currentScreen !is Screen.EventDetail) {
                NavBar(
                    currentScreen = currentScreen,
                    onHome = { currentScreen = Screen.Home },
                    onSearch = { currentScreen = Screen.Search },
                    onAddPost = {
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
    ) { _ ->

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
                            userPreferences?.setRememberMe(rememberMe)
                            loadCurrentUser()
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
                onContinueAsGuest = {
                    isGuest = true
                    currentScreen = Screen.Home
                }
            )

            Screen.Home -> Homepage(
                onLoginClick = { currentScreen = Screen.Login },
                onRegisterClick = { currentScreen = Screen.Registration },
                isUserLoggedIn = authService.getCurrentUserId() != null && !isGuest,
                currentUser = if (!isGuest) currentUser else null
            )

            Screen.Search -> SearchPage(
                currentUser = currentUser,
                onOpenUser = { userId, nickname ->
                    currentScreen = Screen.UserProfile(userId, nickname)
                },
                onOpenHashtag = { hashtag ->
                    currentScreen = Screen.HashtagFeed(hashtag)
                },
                onOpenGroup = { groupId ->
                    currentScreen = Screen.GroupDetail(groupId)
                },
                onOpenEvent = { eventId ->
                    currentScreen = Screen.EventDetail(eventId)
                }
            )

            is Screen.UserProfile -> {
                val s = currentScreen as Screen.UserProfile
                UserProfilePage(
                    userId = s.userId,
                    currentUser = currentUser,
                    onBack = { currentScreen = Screen.Search },
                    onOpenChat = { chatScreen -> currentScreen = chatScreen },
                    onOpenUser = { uid, nickname -> currentScreen = Screen.UserProfile(uid, nickname) }
                )
            }

            is Screen.HashtagFeed -> {
                val s = currentScreen as Screen.HashtagFeed
                HashtagFeedPage(
                    hashtag = s.hashtag,
                    currentUser = currentUser,
                    onBack = { currentScreen = Screen.Search }
                )
            }

            Screen.Messages -> {
                if (!isGuest && authService.getCurrentUserId() != null) {
                    MessagesPage(
                        currentUser = currentUser,
                        onOpenChat = { chatScreen -> currentScreen = chatScreen },
                        onOpenGroup = { groupId -> currentScreen = Screen.GroupDetail(groupId) },
                        onCreateGroup = { currentScreen = Screen.CreateGroup },
                        onOpenEvent = { eventId -> currentScreen = Screen.EventDetail(eventId) },
                        onCreateEvent = { currentScreen = Screen.CreateEvent }
                    )
                } else {
                    currentScreen = Screen.Login
                }
            }

            is Screen.GroupDetail -> {
                val s = currentScreen as Screen.GroupDetail
                GroupDetailPage(
                    groupId = s.groupId,
                    currentUser = currentUser,
                    onBack = { currentScreen = Screen.Messages }
                )
            }

            Screen.CreateGroup -> {
                val user = currentUser
                if (!isGuest && authService.getCurrentUserId() != null && user != null) {
                    CreateGroupScreen(
                        currentUser = user,
                        onCreated = { groupId -> currentScreen = Screen.GroupDetail(groupId) },
                        onCancel = { currentScreen = Screen.Messages }
                    )
                } else {
                    currentScreen = Screen.Login
                }
            }

            is Screen.EventDetail -> {
                val s = currentScreen as Screen.EventDetail
                EventDetailPage(
                    eventId = s.eventId,
                    currentUser = currentUser,
                    onBack = { currentScreen = Screen.Messages }
                )
            }

            Screen.CreateEvent -> {
                val user = currentUser
                if (!isGuest && authService.getCurrentUserId() != null && user != null) {
                    CreateEventScreen(
                        currentUser = user,
                        onCreated = { eventId -> currentScreen = Screen.EventDetail(eventId) },
                        onCancel = { currentScreen = Screen.Messages }
                    )
                } else {
                    currentScreen = Screen.Login
                }
            }

            is Screen.Chat -> {
                val chatScreen = currentScreen as Screen.Chat
                if (!isGuest && authService.getCurrentUserId() != null) {
                    ChatScreen(
                        chat = chatScreen,
                        currentUser = currentUser,
                        onBack = { currentScreen = Screen.Messages }
                    )
                } else {
                    currentScreen = Screen.Login
                }
            }

            Screen.Profile -> {
                if (!isGuest && authService.getCurrentUserId() != null) {
                    ProfilePage(
                        authService = authService,
                        initialUser = currentUser,
                        onLogout = {
                            scope.launch { userPreferences?.setRememberMe(false) }
                            authService.logout()
                            currentUser = null
                            isGuest = false
                            currentScreen = Screen.Login
                        },
                        onEditProfile = { currentScreen = Screen.EditProfile },
                        onOpenAdmin = if (currentUser?.isAdmin == true) {
                            { currentScreen = Screen.Admin }
                        } else null
                    )
                } else {
                    currentScreen = Screen.Login
                }
            }

            Screen.Admin -> {
                if (!isGuest && currentUser?.isAdmin == true) {
                    AdminPage(
                        currentUser = currentUser,
                        onBack = { currentScreen = Screen.Profile }
                    )
                } else {
                    currentScreen = Screen.Profile
                }
            }

            Screen.EditProfile -> {
                val user = currentUser
                if (!isGuest && authService.getCurrentUserId() != null && user != null) {
                    EditProfileScreen(
                        user = user,
                        authService = authService,
                        onSaved = { updatedUser: User ->
                            currentUser = updatedUser
                            currentScreen = Screen.Profile
                        },
                        onCancel = { currentScreen = Screen.Profile }
                    )
                } else {
                    currentScreen = Screen.Login
                }
            }

            Screen.CreatePost -> {
                val user = currentUser
                if (!isGuest && authService.getCurrentUserId() != null && user != null) {
                    CreatePostScreen(
                        currentUser = user,
                        onPostCreated = { currentScreen = Screen .Profile },
                        onCancel = { currentScreen = Screen.Home }
                    )
                } else {
                    currentScreen = Screen.Login
                }
            }
        }
    }
}