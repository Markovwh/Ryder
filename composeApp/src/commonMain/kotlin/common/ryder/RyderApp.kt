package common.ryder

import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import common.ui.pages.*
import common.data.provideAuthService
import common.data.UserPreferences
import common.model.User
import common.ui.pages.components.LocalIsDarkTheme
import common.ui.pages.components.LocalHashtagClickHandler
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
    var isDarkTheme by remember { mutableStateOf(false) }

    // Back stack — push before every forward navigation, pop on back
    val backStack = remember { mutableStateListOf<Screen>() }

    fun navigateTo(screen: Screen) {
        backStack.add(currentScreen)
        currentScreen = screen
    }

    fun navigateBack() {
        currentScreen = backStack.removeLastOrNull() ?: Screen.Home
    }

    fun navigateRoot(screen: Screen) {
        backStack.clear()
        currentScreen = screen
    }

    suspend fun loadCurrentUser() {
        val uid = authService.getCurrentUserId() ?: return
        val result = authService.getUserData(uid)
        if (result.isSuccess) {
            currentUser = result.getOrNull()
            isDarkTheme = userPreferences?.getDarkTheme(uid) ?: false
        }
    }

    // On startup: if remember me was enabled and Firebase still has an active session, skip login
    LaunchedEffect(Unit) {
        if (userPreferences != null) {
            val rememberMe = userPreferences.getRememberMe()
            if (rememberMe && authService.getCurrentUserId() != null) {
                loadCurrentUser()
                navigateRoot(Screen.Home)
            }
        }
    }

    val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

    CompositionLocalProvider(
        LocalIsDarkTheme provides isDarkTheme,
        LocalHashtagClickHandler provides { hashtag ->
            navigateTo(Screen.HashtagFeed(hashtag))
        }
    ) {
    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            bottomBar = {
                if (currentScreen !in listOf(
                        Screen.Registration, Screen.Login, Screen.EditProfile,
                        Screen.CreateGroup, Screen.CreateEvent, Screen.Admin, Screen.Settings
                    )
                    && currentScreen !is Screen.Chat
                    && currentScreen !is Screen.UserProfile
                    && currentScreen !is Screen.HashtagFeed
                    && currentScreen !is Screen.GroupDetail
                    && currentScreen !is Screen.EventDetail
                ) {
                    NavBar(
                        currentScreen = currentScreen,
                        onHome    = { navigateRoot(Screen.Home) },
                        onSearch  = { navigateRoot(Screen.Search) },
                        onAddPost = {
                            if (!isGuest && authService.getCurrentUserId() != null)
                                navigateTo(Screen.CreatePost)
                            else
                                navigateRoot(Screen.Login)
                        },
                        onMessages = {
                            if (!isGuest && authService.getCurrentUserId() != null)
                                navigateRoot(Screen.Messages)
                            else
                                navigateRoot(Screen.Login)
                        },
                        onProfile = {
                            if (!isGuest && authService.getCurrentUserId() != null)
                                navigateRoot(Screen.Profile)
                            else
                                navigateRoot(Screen.Login)
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
                                navigateRoot(Screen.Login)
                            } else {
                                authError = result.exceptionOrNull()?.message
                            }
                        }
                    },
                    onLoginClick = { navigateRoot(Screen.Login) }
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
                                navigateRoot(Screen.Home)
                            } else {
                                authError = result.exceptionOrNull()?.message
                            }
                        }
                    },
                    onForgotPassword = { email ->
                        scope.launch {
                            val result = authService.sendPasswordReset(email)
                            authError = if (result.isSuccess) {
                                "Paroles atiestatīšanas e-pasts nosūtīts."
                            } else {
                                result.exceptionOrNull()?.message
                            }
                        }
                    },
                    onRegisterClick = { navigateRoot(Screen.Registration) },
                    onContinueAsGuest = {
                        isGuest = true
                        navigateRoot(Screen.Home)
                    }
                )

                Screen.Home -> Homepage(
                    onLoginClick = { navigateRoot(Screen.Login) },
                    onRegisterClick = { navigateRoot(Screen.Registration) },
                    isUserLoggedIn = authService.getCurrentUserId() != null && !isGuest,
                    currentUser = if (!isGuest) currentUser else null
                )

                Screen.Search -> SearchPage(
                    currentUser = currentUser,
                    onOpenUser = { userId, nickname ->
                        navigateTo(Screen.UserProfile(userId, nickname))
                    },
                    onOpenHashtag = { hashtag ->
                        navigateTo(Screen.HashtagFeed(hashtag))
                    },
                    onOpenGroup = { groupId ->
                        navigateTo(Screen.GroupDetail(groupId))
                    },
                    onOpenEvent = { eventId ->
                        navigateTo(Screen.EventDetail(eventId))
                    }
                )

                is Screen.UserProfile -> {
                    val s = currentScreen as Screen.UserProfile
                    UserProfilePage(
                        userId = s.userId,
                        currentUser = currentUser,
                        onBack = { navigateBack() },
                        onOpenChat = { chatScreen -> navigateTo(chatScreen) },
                        onOpenUser = { uid, nickname ->
                            navigateTo(Screen.UserProfile(uid, nickname))
                        }
                    )
                }

                is Screen.HashtagFeed -> {
                    val s = currentScreen as Screen.HashtagFeed
                    HashtagFeedPage(
                        hashtag = s.hashtag,
                        currentUser = currentUser,
                        onBack = { navigateBack() }
                    )
                }

                Screen.Messages -> {
                    if (!isGuest && authService.getCurrentUserId() != null) {
                        MessagesPage(
                            currentUser = currentUser,
                            onOpenChat = { chatScreen -> navigateTo(chatScreen) },
                            onOpenUser = { userId ->
                                navigateTo(Screen.UserProfile(userId, ""))
                            },
                            onOpenGroup = { groupId -> navigateTo(Screen.GroupDetail(groupId)) },
                            onCreateGroup = { navigateTo(Screen.CreateGroup) },
                            onOpenEvent = { eventId -> navigateTo(Screen.EventDetail(eventId)) },
                            onCreateEvent = { navigateTo(Screen.CreateEvent) }
                        )
                    } else {
                        navigateRoot(Screen.Login)
                    }
                }

                is Screen.GroupDetail -> {
                    val s = currentScreen as Screen.GroupDetail
                    GroupDetailPage(
                        groupId = s.groupId,
                        currentUser = currentUser,
                        onBack = { navigateBack() }
                    )
                }

                Screen.CreateGroup -> {
                    val user = currentUser
                    if (!isGuest && authService.getCurrentUserId() != null && user != null) {
                        CreateGroupScreen(
                            currentUser = user,
                            onCreated = { groupId ->
                                // Replace CreateGroup with GroupDetail in the flow;
                                // backStack already holds the screen before CreateGroup
                                currentScreen = Screen.GroupDetail(groupId)
                            },
                            onCancel = { navigateBack() }
                        )
                    } else {
                        navigateRoot(Screen.Login)
                    }
                }

                is Screen.EventDetail -> {
                    val s = currentScreen as Screen.EventDetail
                    EventDetailPage(
                        eventId = s.eventId,
                        currentUser = currentUser,
                        onBack = { navigateBack() }
                    )
                }

                Screen.CreateEvent -> {
                    val user = currentUser
                    if (!isGuest && authService.getCurrentUserId() != null && user != null) {
                        CreateEventScreen(
                            currentUser = user,
                            onCreated = { eventId ->
                                currentScreen = Screen.EventDetail(eventId)
                            },
                            onCancel = { navigateBack() }
                        )
                    } else {
                        navigateRoot(Screen.Login)
                    }
                }

                is Screen.Chat -> {
                    val chatScreen = currentScreen as Screen.Chat
                    if (!isGuest && authService.getCurrentUserId() != null) {
                        ChatScreen(
                            chat = chatScreen,
                            currentUser = currentUser,
                            onBack = { navigateBack() },
                            onOpenUser = { userId ->
                                navigateTo(Screen.UserProfile(userId, chatScreen.otherUserNickname))
                            }
                        )
                    } else {
                        navigateRoot(Screen.Login)
                    }
                }

                Screen.Profile -> {
                    if (!isGuest && authService.getCurrentUserId() != null) {
                        ProfilePage(
                            authService = authService,
                            initialUser = currentUser,
                            onEditProfile = { navigateTo(Screen.EditProfile) },
                            onOpenSettings = { navigateTo(Screen.Settings) },
                            onOpenAdmin = { navigateTo(Screen.Admin) },
                            onUserRefreshed = { updatedUser -> currentUser = updatedUser },
                            onOpenUser = { userId, nickname ->
                                navigateTo(Screen.UserProfile(userId, nickname))
                            }
                        )
                    } else {
                        navigateRoot(Screen.Login)
                    }
                }

                Screen.Settings -> {
                    if (!isGuest && authService.getCurrentUserId() != null) {
                        SettingsPage(
                            authService = authService,
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = { dark: Boolean ->
                                isDarkTheme = dark
                                scope.launch { userPreferences?.setDarkTheme(dark, currentUser?.uid) }
                            },
                            onEditProfile = { navigateTo(Screen.EditProfile) },
                            onLogout = {
                                scope.launch { userPreferences?.setRememberMe(false) }
                                currentUser = null
                                isGuest = false
                                isDarkTheme = false
                                navigateRoot(Screen.Login)
                            },
                            onBack = { navigateBack() }
                        )
                    } else {
                        navigateRoot(Screen.Login)
                    }
                }

                Screen.Admin -> {
                    if (!isGuest && currentUser?.isAdmin == true) {
                        AdminPage(
                            currentUser = currentUser,
                            onBack = { navigateBack() }
                        )
                    } else {
                        navigateRoot(Screen.Profile)
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
                                navigateBack()
                            },
                            onCancel = { navigateBack() }
                        )
                    } else {
                        navigateRoot(Screen.Login)
                    }
                }

                Screen.CreatePost -> {
                    val user = currentUser
                    if (!isGuest && authService.getCurrentUserId() != null && user != null) {
                        CreatePostScreen(
                            currentUser = user,
                            onPostCreated = { navigateRoot(Screen.Profile) },
                            onCancel = { navigateBack() }
                        )
                    } else {
                        navigateRoot(Screen.Login)
                    }
                }
            }
        }
    }
    } // CompositionLocalProvider
}
