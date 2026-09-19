package com.zaaam.zreming.ui.navigation

private fun enc(value: String): String =
    java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object BeliAccount : Screen("beli_account")
    data object PulihkanAccount : Screen("pulihkan_account")
    data object AboutUs : Screen("about_us")
    data object Home : Screen("home")
    data object NobarHub : Screen("nobar_hub")
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
        ): String {
            // Semua nilai WAJIB di-encode dan gak boleh kosong. Kalau kosong,
            // route-nya jadi "contentId=" dan Navigation gak bisa nemu tujuan
            // -> IllegalArgumentException alias app force close.
            val id = enc(contentId.ifBlank { "0" })
            val title = enc(contentTitle.ifBlank { "Nobar" })
            val poster = enc(posterUrl.ifBlank { "-" })
            return "nobar_invite?contentId=$id&contentTitle=$title&isTv=$isTv" +
                "&season=$season&episode=$episode&posterUrl=$poster"
        }
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
        ): String {
            val t = enc(title.ifBlank { "Player" })
            val id = enc(contentId.ifBlank { "0" })
            val poster = enc(posterUrl.ifBlank { "-" })
            val room = enc(roomId.ifBlank { "-" })
            return "player?title=$t&contentId=$id&isTv=$isTv&season=$season" +
                "&episode=$episode&startPosSec=$startPosSec&posterUrl=$poster&roomId=$room"
        }
    }
}
