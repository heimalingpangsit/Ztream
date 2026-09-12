package com.zaaam.zreming.ui.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.zaaam.zreming.data.local.SessionManager
import com.zaaam.zreming.ui.auth.LoginScreen
import com.zaaam.zreming.ui.chat.ChatListScreen
import com.zaaam.zreming.ui.chat.ChatScreen
import com.zaaam.zreming.ui.detail.DetailScreen
import com.zaaam.zreming.ui.detail.DetailViewModel
import com.zaaam.zreming.ui.home.HomeScreen
import com.zaaam.zreming.ui.home.HomeViewModel
import com.zaaam.zreming.ui.mylist.MyListScreen
import com.zaaam.zreming.ui.mylist.MyListViewModel
import com.zaaam.zreming.ui.owner.OwnerPanelScreen
import com.zaaam.zreming.ui.player.PlayerScreen
import com.zaaam.zreming.ui.player.PlayerViewModel
import com.zaaam.zreming.ui.profile.ProfileScreen
import com.zaaam.zreming.ui.search.SearchScreen
import com.zaaam.zreming.ui.search.SearchViewModel
import com.zaaam.zreming.ui.social.DiscoverUsersScreen
import com.zaaam.zreming.ui.social.FollowListScreen
import com.zaaam.zreming.ui.social.UserProfileScreen
import com.zaaam.zreming.ui.splash.SplashScreen
import com.zaaam.zreming.ui.theme.CardDark
import com.zaaam.zreming.ui.theme.PrimaryRed
import com.zaaam.zreming.ui.theme.TextDim

@Composable
fun AppNavigation(sessionManager: SessionManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.MyList.route,
        Screen.Profile.route,
        Screen.ChatList.route,
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    color = com.zaaam.zreming.ui.theme.CardDark.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = androidx.compose.ui.Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(64.dp)
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == Screen.Home.route,
                            onClick = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                            label = { Text("Beranda", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryRed,
                                selectedTextColor = PrimaryRed,
                                indicatorColor = Color.White.copy(alpha = 0.1f),
                                unselectedIconColor = TextDim,
                                unselectedTextColor = TextDim
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == Screen.Search.route,
                            onClick = {
                                navController.navigate(Screen.Search.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Pencarian") },
                            label = { Text("Pencarian", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryRed,
                                selectedTextColor = PrimaryRed,
                                indicatorColor = Color.White.copy(alpha = 0.1f),
                                unselectedIconColor = TextDim,
                                unselectedTextColor = TextDim
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == Screen.MyList.route,
                            onClick = {
                                navController.navigate(Screen.MyList.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Bookmark, contentDescription = "Daftarku") },
                            label = { Text("Daftarku", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryRed,
                                selectedTextColor = PrimaryRed,
                                indicatorColor = Color.White.copy(alpha = 0.1f),
                                unselectedIconColor = TextDim,
                                unselectedTextColor = TextDim
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == Screen.ChatList.route,
                            onClick = {
                                navController.navigate(Screen.ChatList.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Chat, contentDescription = "Pesan") },
                            label = { Text("Pesan", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryRed,
                                selectedTextColor = PrimaryRed,
                                indicatorColor = Color.White.copy(alpha = 0.1f),
                                unselectedIconColor = TextDim,
                                unselectedTextColor = TextDim
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == Screen.Profile.route,
                            onClick = {
                                navController.navigate(Screen.Profile.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                            label = { Text("Profil", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryRed,
                                selectedTextColor = PrimaryRed,
                                indicatorColor = Color.White.copy(alpha = 0.1f),
                                unselectedIconColor = TextDim,
                                unselectedTextColor = TextDim
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val postSplashDestination = if (sessionManager.isLoggedIn()) Screen.Home.route else Screen.Login.route

        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onFinished = {
                        navController.navigate(postSplashDestination) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                val viewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = viewModel,
                    onContentClick = { slug ->
                        navController.navigate(Screen.Detail.createRoute(slug))
                    },
                    onPlayClick = { title, contentId, isTv, season, episode, startPosSec, posterUrl ->
                        navController.navigate(Screen.Player.createRoute(title, contentId, isTv, season, episode, startPosSec, posterUrl))
                    }
                )
            }

            composable(Screen.Search.route) {
                val viewModel: SearchViewModel = hiltViewModel()
                SearchScreen(
                    viewModel = viewModel,
                    onContentClick = { slug ->
                        navController.navigate(Screen.Detail.createRoute(slug))
                    }
                )
            }

            composable(Screen.MyList.route) {
                val viewModel: MyListViewModel = hiltViewModel()
                MyListScreen(
                    viewModel = viewModel,
                    onContentClick = { slug ->
                        navController.navigate(Screen.Detail.createRoute(slug))
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onOpenOwnerPanel = { navController.navigate(Screen.OwnerPanel.route) },
                    onLoggedOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.OwnerPanel.route) {
                OwnerPanelScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Discover.route) {
                DiscoverUsersScreen(
                    onBack = { navController.popBackStack() },
                    onOpenProfile = { username -> navController.navigate(Screen.UserProfile.createRoute(username)) }
                )
            }

            composable(Screen.ChatList.route) {
                ChatListScreen(
                    onOpenChat = { username -> navController.navigate(Screen.Chat.createRoute(username)) },
                    onOpenDiscover = { navController.navigate(Screen.Discover.route) }
                )
            }

            composable(
                Screen.UserProfile.route,
                arguments = listOf(navArgument("username") { type = NavType.StringType })
            ) {
                UserProfileScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChat = { username -> navController.navigate(Screen.Chat.createRoute(username)) },
                    onOpenFollowers = { username -> navController.navigate(Screen.FollowList.createRoute(username, "followers")) },
                    onOpenFollowing = { username -> navController.navigate(Screen.FollowList.createRoute(username, "following")) }
                )
            }

            composable(
                Screen.FollowList.route,
                arguments = listOf(
                    navArgument("username") { type = NavType.StringType },
                    navArgument("direction") { type = NavType.StringType }
                )
            ) {
                FollowListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenProfile = { username -> navController.navigate(Screen.UserProfile.createRoute(username)) }
                )
            }

            composable(
                Screen.Chat.route,
                arguments = listOf(navArgument("username") { type = NavType.StringType })
            ) {
                ChatScreen(
                    myUsername = sessionManager.getUser()?.username.orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("slug") { type = NavType.StringType })
            ) {
                val viewModel: DetailViewModel = hiltViewModel()
                DetailScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onPlayClick = { title, contentId, isTv, season, episode, startPosSec, posterUrl ->
                        navController.navigate(Screen.Player.createRoute(title, contentId, isTv, season, episode, startPosSec, posterUrl))
                    },
                    onNobarClick = { contentId, contentTitle, isTv, season, episode, posterUrl ->
                        navController.navigate(
                            Screen.NobarInvite.createRoute(contentId, contentTitle, isTv, season, episode, posterUrl)
                        )
                    }
                )
            }

            composable(
                route = Screen.NobarInvite.route,
                arguments = listOf(
                    navArgument("contentId") { type = NavType.StringType },
                    navArgument("contentTitle") { type = NavType.StringType; defaultValue = "Nobar" },
                    navArgument("isTv") { type = NavType.BoolType; defaultValue = false },
                    navArgument("season") { type = NavType.IntType; defaultValue = 1 },
                    navArgument("episode") { type = NavType.IntType; defaultValue = 1 },
                    navArgument("posterUrl") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                com.zaaam.zreming.ui.nobar.NobarInviteScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRoom = { roomId ->
                        val contentId = backStackEntry.arguments?.getString("contentId") ?: "0"
                        val contentTitle = backStackEntry.arguments?.getString("contentTitle") ?: "Nobar"
                        val isTv = backStackEntry.arguments?.getBoolean("isTv") ?: false
                        val season = backStackEntry.arguments?.getInt("season") ?: 1
                        val episode = backStackEntry.arguments?.getInt("episode") ?: 1
                        val posterUrl = backStackEntry.arguments?.getString("posterUrl") ?: ""
                        navController.navigate(
                            Screen.Player.createRoute(contentTitle, contentId, isTv, season, episode, 0L, posterUrl, roomId)
                        ) {
                            popUpTo(Screen.NobarInvite.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType; defaultValue = "Player" },
                    navArgument("contentId") { type = NavType.StringType; defaultValue = "0" },
                    navArgument("isTv") { type = NavType.BoolType; defaultValue = false },
                    navArgument("season") { type = NavType.IntType; defaultValue = 1 },
                    navArgument("episode") { type = NavType.IntType; defaultValue = 1 },
                    navArgument("startPosSec") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("posterUrl") { type = NavType.StringType; defaultValue = "" },
                    navArgument("roomId") { type = NavType.StringType; defaultValue = "" }
                )
            ) {
                val viewModel: PlayerViewModel = hiltViewModel()
                PlayerScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
