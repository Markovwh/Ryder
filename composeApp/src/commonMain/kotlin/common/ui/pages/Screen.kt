package common.ui.pages

sealed class Screen {
    object Home : Screen()
    object Search : Screen()
    object Messages : Screen()
    object Profile : Screen()
    object Registration : Screen()
    object Login : Screen()
    object CreatePost : Screen()
    object EditProfile : Screen()
}