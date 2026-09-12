package com.zaaam.zreming.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object MyList : Screen("mylist")
    data object Profile : Screen("profile")
    data object OwnerPanel : Screen("owner_panel")
    data object Discover : Screen("discover")
    data object ChatList : Screen("chat_list")
    data object UserProfile : Screen("user/{username}") {
        fun createRoute(username: String) = "user/$username"
    }
    data object FollowList : Screen("user/{username}/{direction}") {
        fun createRoute(username: String, direction: String) = "user/$username/$direction"
    }
    data object Chat : Screen("chat/{username}") {
        fun createRoute(username: String) = "chat/$username"
    }
    data object Detail : Screen("detail/{slug}") {
        fun createRoute(slug: String) = "detail/${java.net.URLEncoder.encode(slug, "UTF-8")}"
    }
    data object NobarInvite : Screen("nobar_invite?contentId={contentId}&contentTitle={contentTitle}&isTv={isTv}&season={season}&episode={episode}&posterUrl={posterUrl}") {
        fun createRoute(
            contentId: String,
            contentTitle: String,
            isTv: Boolean = false,
            season: Int = 1,
            episode: Int = 1,
            posterUrl: String = "",
        ) = "nobar_invite?contentId=$contentId&contentTitle=${java.net.URLEncoder.encode(contentTitle, "UTF-8")}&isTv=$isTv&season=$season&episode=$episode&posterUrl=${java.net.URLEncoder.encode(posterUrl, "UTF-8")}"
    }
    data object Player : Screen("player?title={title}&contentId={contentId}&isTv={isTv}&season={season}&episode={episode}&startPosSec={startPosSec}&posterUrl={posterUrl}&roomId={roomId}") {
        fun createRoute(
            title: String,
            contentId: String,
            isTv: Boolean = false,
            season: Int = 1,
            episode: Int = 1,
            startPosSec: Long = 0L,
            posterUrl: String = "",
            roomId: String = ""
        ) = "player?title=${java.net.URLEncoder.encode(title, "UTF-8")}&contentId=$contentId&isTv=$isTv&season=$season&episode=$episode&startPosSec=$startPosSec&posterUrl=${java.net.URLEncoder.encode(posterUrl, "UTF-8")}&roomId=${java.net.URLEncoder.encode(roomId, "UTF-8")}"
    }
}
