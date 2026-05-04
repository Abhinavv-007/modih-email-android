package com.modih.mail.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import com.modih.mail.ui.screens.admin.AdminLoginScreen
import com.modih.mail.ui.screens.admin.AdminPanelScreen
import com.modih.mail.ui.screens.auth.LoginScreen
import com.modih.mail.ui.screens.auth.SignUpScreen
import com.modih.mail.ui.screens.dashboard.DashboardScreen
import com.modih.mail.ui.screens.developer.DeveloperScreen
import com.modih.mail.ui.screens.docs.PrivacyPolicyScreen
import com.modih.mail.ui.screens.docs.TermsScreen
import com.modih.mail.ui.screens.docs.AboutScreen
import com.modih.mail.ui.screens.home.HomeScreen
import com.modih.mail.ui.screens.inbox.InboxScreen
import com.modih.mail.ui.screens.inbox.MessageDetailScreen
import com.modih.mail.ui.screens.pricing.PricingScreen
import com.modih.mail.ui.screens.settings.SettingsScreen
import com.modih.mail.ui.screens.settings.AccountScreen
import com.modih.mail.ui.screens.settings.AliasManagementScreen
import com.modih.mail.ui.screens.support.SupportScreen
import com.modih.mail.ui.theme.*

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Dashboard : Screen("dashboard")
    data object Inbox : Screen("inbox")
    data object Pricing : Screen("pricing")
    data object Settings : Screen("settings")
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object Developer : Screen("developer")
    data object Privacy : Screen("privacy")
    data object Terms : Screen("terms")
    data object About : Screen("about")
    data object Account : Screen("account")
    data object AliasManagement : Screen("alias_management")
    data object Support : Screen("support")
    data object AdminLogin : Screen("admin_login")
    data object AdminPanel : Screen("admin_panel")
    data object MessageDetail : Screen("message/{messageId}") {
        fun createRoute(messageId: String) = "message/$messageId"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Dashboard, "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomNavItem(Screen.Inbox, "Inbox", Icons.Filled.Email, Icons.Outlined.Email),
    BottomNavItem(Screen.Pricing, "Plans", Icons.Filled.Star, Icons.Outlined.Star),
    BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun ModihNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        // v1.3: snappier nav. Fade ~180ms in / ~120ms out, with a tiny
        // horizontal nudge instead of a full 100px slide. Material's
        // "responsive" guidance is 150-200ms for screen transitions.
        enterTransition = { fadeIn(animationSpec = tween(180)) + slideInHorizontally(initialOffsetX = { it / 12 }) },
        exitTransition = { fadeOut(animationSpec = tween(120)) },
        popEnterTransition = { fadeIn(animationSpec = tween(180)) + slideInHorizontally(initialOffsetX = { -it / 12 }) },
        popExitTransition = { fadeOut(animationSpec = tween(120)) }
    ) {
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Dashboard.route) { DashboardScreen(navController) }
        composable(Screen.Inbox.route) { InboxScreen(navController) }
        composable(Screen.Pricing.route) { PricingScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.SignUp.route) { SignUpScreen(navController) }
        composable(Screen.Developer.route) { DeveloperScreen(navController) }
        composable(Screen.Privacy.route) { PrivacyPolicyScreen(navController) }
        composable(Screen.Terms.route) { TermsScreen(navController) }
        composable(Screen.About.route) { AboutScreen(navController) }
        composable(Screen.Account.route) { AccountScreen(navController) }
        composable(Screen.AliasManagement.route) { AliasManagementScreen(navController) }
        composable(Screen.Support.route) { SupportScreen(navController) }
        composable(Screen.AdminLogin.route) { AdminLoginScreen(navController) }
        composable(Screen.AdminPanel.route) { AdminPanelScreen(navController) }
        composable(
            Screen.MessageDetail.route,
            arguments = listOf(navArgument("messageId") { defaultValue = "" })
        ) { backStackEntry ->
            val messageId = backStackEntry.arguments?.getString("messageId") ?: ""
            MessageDetailScreen(navController, messageId)
        }
    }
}

@Composable
fun ModihBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on non-tab screens
    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }
    if (!showBottomBar) return

    NavigationBar(
        modifier = modifier,
        containerColor = BgPrimary.copy(alpha = 0.95f),
        contentColor = TextMuted,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = Inter
                    )
                },
                selected = selected,
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentGold,
                    selectedTextColor = AccentGold,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = AccentGoldSubtle
                )
            )
        }
    }
}
