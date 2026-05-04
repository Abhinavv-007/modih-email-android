package com.modih.mail.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.modih.mail.data.local.PreferencesManager
import com.modih.mail.data.local.SavedInbox
import com.modih.mail.data.model.Plan
import com.modih.mail.data.model.UsageStats
import com.modih.mail.data.repository.MailRepository
import com.modih.mail.ui.components.GlassCard
import com.modih.mail.ui.components.GlassPill
import com.modih.mail.ui.components.GoldButton
import com.modih.mail.ui.components.PlanBadge
import com.modih.mail.ui.components.rememberUserPlanState
import com.modih.mail.ui.navigation.Screen
import com.modih.mail.ui.theme.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val prefs = remember { PreferencesManager(context.applicationContext) }
    val repo = remember { MailRepository() }

    val userPlanState = rememberUserPlanState()
    val plan = userPlanState.plan
    val isLoggedIn = userPlanState.isLoggedIn

    val savedInbox by prefs.currentInboxFlow.collectAsState(initial = null)
    var devUsage by remember { mutableStateOf<UsageStats?>(null) }
    var greeting by remember { mutableStateOf(greetingFor(System.currentTimeMillis())) }

    LaunchedEffect(plan, isLoggedIn) {
        greeting = greetingFor(System.currentTimeMillis())
        if (isLoggedIn && plan == Plan.DEVELOPER) {
            val token = auth.currentUser?.getIdToken(false)?.await()?.token
            if (!token.isNullOrBlank()) {
                repo.getDeveloperUsage(token).onSuccess { devUsage = it }
            }
        } else {
            devUsage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    greeting,
                    fontSize = 13.sp,
                    color = TextMuted,
                    fontFamily = Inter
                )
                val name = userPlanState.email?.split("@")?.firstOrNull()?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                } ?: "to MODiH"
                Text(
                    "Welcome back, $name",
                    fontSize = 24.sp,
                    fontFamily = CormorantGaramond,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1
                )
            }
            PlanBadge(plan.name.lowercase())
        }

        Spacer(Modifier.height(24.dp))

        // ── Plan card ──────────────────────────────────────────────
        PlanCard(plan = plan, isLoggedIn = isLoggedIn) {
            if (isLoggedIn) navController.navigate(Screen.Pricing.route)
            else navController.navigate(Screen.Login.route)
        }

        Spacer(Modifier.height(16.dp))

        // ── Active inbox ───────────────────────────────────────────
        ActiveInboxCard(
            savedInbox = savedInbox,
            onOpen = { navController.navigate(Screen.Inbox.route) },
            onCreate = { navController.navigate(Screen.Inbox.route) }
        )

        Spacer(Modifier.height(16.dp))

        // ── Quick actions ──────────────────────────────────────────
        Text(
            "QUICK ACTIONS",
            fontSize = 11.sp,
            color = TextMuted,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.MailOutline,
                label = "New inbox",
                tint = AccentGold
            ) { navController.navigate(Screen.Inbox.route) }
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Bookmark,
                label = "Aliases",
                tint = AccentGoldLight
            ) { navController.navigate(Screen.AliasManagement.route) }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Code,
                label = "Developer",
                tint = ProPurple,
                enabled = plan == Plan.DEVELOPER
            ) { navController.navigate(Screen.Developer.route) }
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Star,
                label = "Plans",
                tint = AccentGold
            ) { navController.navigate(Screen.Pricing.route) }
        }

        Spacer(Modifier.height(20.dp))

        // ── Usage card (Developer only) ────────────────────────────
        AnimatedVisibility(
            visible = plan == Plan.DEVELOPER && devUsage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            DeveloperUsageCard(devUsage = devUsage) { navController.navigate(Screen.Developer.route) }
        }

        if (plan == Plan.DEVELOPER && devUsage == null && isLoggedIn) {
            Spacer(Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = AccentGold
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Loading API usage…",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Tips ───────────────────────────────────────────────────
        TipsCarousel()

        Spacer(Modifier.height(40.dp))
    }
}

private fun greetingFor(epochMillis: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMillis }
    return when (cal[java.util.Calendar.HOUR_OF_DAY]) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
}

@Composable
private fun PlanCard(plan: Plan, isLoggedIn: Boolean, onAction: () -> Unit) {
    val gradient = when (plan) {
        Plan.PRO -> Brush.linearGradient(listOf(AccentGold.copy(alpha = 0.20f), AccentGoldDim.copy(alpha = 0.05f)))
        Plan.DEVELOPER -> Brush.linearGradient(listOf(ProPurple.copy(alpha = 0.20f), ProPurpleDim.copy(alpha = 0.05f)))
        Plan.FREE -> Brush.linearGradient(listOf(GlassBgCard, GlassBgSubtle))
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(22.dp)),
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.background(gradient).padding(20.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassPill(text = "Your plan", icon = "✦")
                    Spacer(Modifier.weight(1f))
                    PlanBadge(plan.name.lowercase())
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    when (plan) {
                        Plan.FREE -> "Free · 3 inboxes per day · 3-hour TTL"
                        Plan.PRO -> "Pro · 25 inboxes per day · 7-day TTL"
                        Plan.DEVELOPER -> "Developer · 5,000 creates / 50,000 reads per month"
                    },
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontFamily = Inter
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoldButton(
                        text = if (!isLoggedIn) "Sign in to unlock"
                        else if (plan == Plan.FREE) "Upgrade to Pro" else "Manage plan",
                        onClick = onAction,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveInboxCard(
    savedInbox: SavedInbox?,
    onOpen: () -> Unit,
    onCreate: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                tint = AccentGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Active inbox",
                fontSize = 12.sp,
                color = TextMuted,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(12.dp))

        if (savedInbox == null) {
            Text(
                "No active inbox right now.",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Create one to start receiving disposable emails instantly.",
                color = TextMuted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(14.dp))
            GoldButton(
                text = "Create inbox",
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                savedInbox.email,
                color = AccentGold,
                fontSize = 18.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.height(6.dp))
            val expiresIn = remember(savedInbox.expiresAt) {
                val seconds = savedInbox.expiresAt - System.currentTimeMillis() / 1000
                when {
                    seconds <= 0 -> "Expired"
                    seconds < 60 -> "$seconds s left"
                    seconds < 3600 -> "${seconds / 60} min left"
                    seconds < 86400 -> "${seconds / 3600} h left"
                    else -> "${seconds / 86400} d left"
                }
            }
            Text(
                expiresIn,
                color = TextMuted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(14.dp))
            GoldButton(
                text = "Open inbox",
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Email
            )
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) GlassBgCard else GlassBgSubtle,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = tint.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                label,
                color = if (enabled) TextPrimary else TextDim,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = Inter
            )
            if (!enabled) {
                Spacer(Modifier.height(2.dp))
                Text("Developer plan only", fontSize = 11.sp, color = TextDim)
            }
        }
    }
}

@Composable
private fun DeveloperUsageCard(devUsage: UsageStats?, onOpen: () -> Unit) {
    val usage = devUsage ?: return
    val createsPct = if (usage.inboxesLimit > 0) usage.inboxesCreated.toFloat() / usage.inboxesLimit else 0f
    val readsPct = if (usage.readsLimit > 0) usage.readsUsed.toFloat() / usage.readsLimit else 0f
    val resetsAt = remember(usage.resetsAt) {
        if (usage.resetsAt > 0) SimpleDateFormat("MMM dd", Locale.getDefault())
            .format(Date(usage.resetsAt * 1000))
        else "this month"
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "API usage this month",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text("Resets $resetsAt", fontSize = 11.sp, color = TextMuted)
        }
        Spacer(Modifier.height(14.dp))
        UsageBar(
            label = "Inbox creates",
            used = usage.inboxesCreated,
            total = usage.inboxesLimit,
            fraction = createsPct,
            tint = AccentGold
        )
        Spacer(Modifier.height(12.dp))
        UsageBar(
            label = "Message reads",
            used = usage.readsUsed,
            total = usage.readsLimit,
            fraction = readsPct,
            tint = ProPurple
        )
        Spacer(Modifier.height(14.dp))
        GoldButton(
            text = "Open Developer panel",
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun UsageBar(label: String, used: Int, total: Int, fraction: Float, tint: Color) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "usage-bar"
    )

    Column {
        Row {
            Text(label, color = TextSecondary, fontSize = 13.sp, fontFamily = Inter)
            Spacer(Modifier.weight(1f))
            Text(
                "$used / $total",
                color = TextMuted,
                fontSize = 12.sp,
                fontFamily = Inter
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GlassBgSubtle)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(listOf(tint.copy(alpha = 0.6f), tint))
                    )
            )
        }
    }
}

private val tipMessages = listOf(
    "Pro tip — long-press an OTP code in the inbox to copy it instantly.",
    "Need a custom prefix? Upgrade to Pro and pick anything @modih.in.",
    "Developer plan unlocks 5,000 inbox creates and 50,000 reads every month.",
    "Saved aliases are stored locally — they never leave your device until you generate."
)

@Composable
private fun TipsCarousel() {
    val index = remember { (Math.abs(System.currentTimeMillis().toInt()) % tipMessages.size) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = AccentGoldSubtle,
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("✦", fontSize = 22.sp, color = AccentGold)
            Spacer(Modifier.width(12.dp))
            Text(
                tipMessages[index],
                color = AccentGoldLight,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Start,
                fontFamily = Inter
            )
        }
    }
}
