package com.modih.mail.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.modih.mail.data.local.PreferencesManager
import com.modih.mail.data.model.Plan
import com.modih.mail.ui.components.*
import com.modih.mail.ui.navigation.Screen
import com.modih.mail.ui.theme.*
import com.modih.mail.util.Constants
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val prefs = remember { PreferencesManager(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val user = auth.currentUser
    val isLoggedIn = user != null
    val userPlanState = rememberUserPlanState()
    val currentPlan = userPlanState.plan

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            "Settings",
            fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic, fontSize = 32.sp, color = TextPrimary
        )

        Spacer(Modifier.height(24.dp))

        // Account section
        if (isLoggedIn) {
            Text("ACCOUNT", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar circle
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(50),
                        color = AccentGoldSubtle
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                (user?.email?.firstOrNull() ?: '?').uppercase(),
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentGold
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            user?.email?.split("@")?.firstOrNull()?.take(20) ?: "Account",
                            fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary
                        )
                        Text(user?.email ?: "", fontSize = 13.sp, color = TextMuted)
                    }
                    if (userPlanState.isLoading && currentPlan == Plan.FREE) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AccentGold
                        )
                    } else {
                        PlanBadge(currentPlan.name.lowercase())
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsItem(
                icon = Icons.Outlined.Person,
                title = "Account Details",
                subtitle = "Email, plan, manage account",
                onClick = { navController.navigate(Screen.Account.route) }
            )
            SettingsItem(
                icon = Icons.Outlined.Bookmark,
                title = "Reserved Aliases",
                subtitle = "Manage your reserved email aliases",
                onClick = { navController.navigate(Screen.AliasManagement.route) }
            )
            if (currentPlan == Plan.DEVELOPER) {
                SettingsItem(
                    icon = Icons.Outlined.Code,
                    title = "Developer API",
                    subtitle = "API keys, usage, endpoints",
                    onClick = { navController.navigate(Screen.Developer.route) }
                )
            }
        } else {
            // Not logged in
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🔒", fontSize = 36.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Sign in for more features", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Unlock Pro features, reserved aliases, custom prefixes, and more.",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    GoldButton("Sign In", { navController.navigate(Screen.Login.route) }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    GhostButton("Create Account", { navController.navigate(Screen.SignUp.route) }, Modifier.fillMaxWidth())
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // General section
        Text("GENERAL", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))

        SettingsItem(
            icon = Icons.Outlined.Star,
            title = "Plans & Pricing",
            subtitle = if (userPlanState.isLoading && isLoggedIn) {
                "Syncing your plan..."
            } else {
                "View and compare plans"
            },
            onClick = { navController.navigate(Screen.Pricing.route) }
        )

        Spacer(Modifier.height(28.dp))

        // Legal section
        Text("LEGAL & INFO", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))

        SettingsItem(
            icon = Icons.Outlined.Shield,
            title = "Privacy Policy",
            subtitle = "How we handle your data",
            onClick = { navController.navigate(Screen.Privacy.route) }
        )
        SettingsItem(
            icon = Icons.Outlined.Description,
            title = "Terms of Service",
            subtitle = "Usage terms and conditions",
            onClick = { navController.navigate(Screen.Terms.route) }
        )
        SettingsItem(
            icon = Icons.Outlined.Info,
            title = "About Modih Mail",
            subtitle = "Version, credits, links",
            onClick = { navController.navigate(Screen.About.route) }
        )
        SettingsItem(
            icon = Icons.Outlined.Email,
            title = "Contact Support",
            subtitle = "We reply within 24 hours",
            onClick = { navController.navigate(Screen.Support.route) }
        )
        SettingsItem(
            icon = Icons.Outlined.AdminPanelSettings,
            title = "Admin",
            subtitle = "Restricted — secret required",
            onClick = { navController.navigate(Screen.AdminLogin.route) }
        )

        Spacer(Modifier.height(28.dp))

        // Links section
        Text("CONNECT", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))

        SettingsItem(
            icon = Icons.Outlined.Language,
            title = "Visit modih.in",
            subtitle = "Open in browser",
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.BASE_URL)))
            }
        )
        SettingsItem(
            icon = Icons.Outlined.Person,
            title = "LinkedIn",
            subtitle = "Connect with Abhinav",
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.LINKEDIN_URL)))
            }
        )

        // Sign out
        if (isLoggedIn) {
            Spacer(Modifier.height(28.dp))
            SettingsItem(
                icon = Icons.AutoMirrored.Outlined.Logout,
                title = "Sign Out",
                subtitle = "Log out of your account",
                titleColor = Danger,
                onClick = {
                    scope.launch {
                        prefs.clearUser()
                        auth.signOut()
                        navController.navigate(Screen.Home.route) { popUpTo(0) }
                    }
                }
            )
        }

        Spacer(Modifier.height(40.dp))

        // Footer
        Text(
            "Modih Mail v1.0.0",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 12.sp, color = TextDim
        )
        Text(
            "Made with care by Abhinav",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 12.sp, color = TextDim
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: Color = TextPrimary
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = GlassBgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, null,
                modifier = Modifier.size(22.dp),
                tint = if (titleColor == Danger) Danger else AccentGold
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = titleColor, fontFamily = Inter)
                Text(subtitle, fontSize = 12.sp, color = TextMuted)
            }
            Icon(
                Icons.Outlined.ChevronRight, null,
                modifier = Modifier.size(20.dp),
                tint = TextDim
            )
        }
    }
}
